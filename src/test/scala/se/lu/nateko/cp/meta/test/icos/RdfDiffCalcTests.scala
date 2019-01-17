package se.lu.nateko.cp.meta.test.icos

import java.net.URI

import org.eclipse.rdf4j.model.Statement
import org.scalatest.FunSpec

import se.lu.nateko.cp.meta.core.data.Envri
import se.lu.nateko.cp.meta.core.data.EnvriConfig
import se.lu.nateko.cp.meta.icos._
import se.lu.nateko.cp.meta.instanceserver.Rdf4jInstanceServer
import se.lu.nateko.cp.meta.services.CpVocab
import se.lu.nateko.cp.meta.services.CpmetaVocab
import se.lu.nateko.cp.meta.utils.rdf4j.Loading
import org.scalatest.GivenWhenThen
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import RdfDiffCalcTests._

class RdfDiffCalcTests extends FunSpec with GivenWhenThen{

	implicit val envriConfs = Map(
		Envri.ICOS -> EnvriConfig(null, null, null, new URI("http://test.icos.eu/resources/"))
	)

	type A = ATC.type
	import TcConf.AtcConf.{makeId => aId}

	val jane = Person[A]("Jane_Doe", aId("pers_0"), "Jane", "Doe", Some("jane.doe@icos-ri.eu"))
	val airCpStation = CpMobileStation[A]("AIR1", aId("43"), "Airplane 1", "AIR1", None)

	def atcInitSnap(pi: Person[A]): TcState[A] = {
		val airTcStation = new TcStation[A](airCpStation, OneOrMorePis(pi))
		new TcState[A](stations = Seq(airTcStation), roles = Nil, instruments = Nil)
	}

	describe("person name change"){

		Given("starting with an empty state with no own CP statements")

		val state = init(Nil, _ => Nil)

		When("an ATC-state snapshot with single station is inserted")

		val initUpdates = state.calc.calcDiff(atcInitSnap(jane)).toIndexedSeq

		it("Then it results in expected sequence of RDF updates"){
			assert(initUpdates.forall(_.isAssertion))
			assert(initUpdates.size >= 13) //the number may change if metadata model changes
		}

		state.tcServer.applyAll(initUpdates)

		And("reading current TC state back produces expected value")

		it("(has the expected PI, the station and the role)"){
			val s = state.reader.getCurrentState[A]
			assert(s.stations.size === 1)
			assert(s.stations.head === airCpStation)
			assert(s.instruments.isEmpty)
			assert(s.roles.size === 1)
			val memb = s.roles.head
			assert(memb.start.isEmpty) //init state was empty, so cannot know when the role was assumed first
			assert(memb.stop.isEmpty) //just created, so cannot have ended
			assert(memb.role.role === PI)
			assert(memb.role.org === airCpStation)
			assert(memb.role.holder === jane)
		}

		When("afterwards a new snapshot comes with person's last name (and consequently cpId) changed")

		val jane2 = jane.copy(cpId = "Jane_Smith", lName = "Smith")

		val nameUpdates = state.calc.calcDiff(atcInitSnap(jane2)).toIndexedSeq

		it("Then only name-changing updates are applied"){
			assert(nameUpdates.size === 2)

			val gist = nameUpdates.map{upd =>
				upd.statement.getObject.stringValue -> upd.isAssertion
			}.toMap

			assert(gist === Map("Doe" -> false, "Smith" -> true))
		}
	}

	describe("PI change"){

		Given("starting with a single station with single PI and no own CP statements")

		val state = init(Nil, _ => Nil)
		state.tcServer.applyAll(state.calc.calcDiff(atcInitSnap(jane)))

		When("a new snapshot comes where the PI has changed")

		val john = Person[A]("John_Brown", aId("pers_1"), "John", "Brown", Some("john.brown@icos-ri.eu"))
		val piUpdates = state.calc.calcDiff(atcInitSnap(john)).toIndexedSeq
		state.tcServer.applyAll(piUpdates)

		it("Then previous PI's membership is ended and the new ones' is created and started"){
			assert(piUpdates.size >= 11)
			val membs = state.reader.getCurrentState[A].roles
			assert(membs.size === 2)
			val johnMemb = membs.find(_.role.holder == john).get
			val janeMemb = membs.find(_.role.holder == jane).get
			assert(johnMemb.start.isDefined)
			assert(johnMemb.stop.isEmpty)
			assert(janeMemb.start.isEmpty)
			assert(janeMemb.stop.isDefined)
		}

	}

	describe("CP takes over a definition of a metadata entity"){

		Given("starting with a single station with single PI and no own CP statements")

		val state = init(Nil, _ => Nil)
		val initSnap = atcInitSnap(jane)
		state.tcServer.applyAll(state.calc.calcDiff(initSnap))

		When("CP creates a new person metadata and associates it with the exising TC person metadata")

		val cpJane = Person[A]("Jane_CP", jane.tcId, "Jane", "CP", Some("jane.cp@icos-cp.eu"))
		state.cpServer.addAll(state.maker.getStatements(cpJane))

		val updates = state.calc.calcDiff(initSnap).toIndexedSeq //no change in the TC picture
		state.tcServer.applyAll(updates)

		it("Then arrival of an unchanged TC metadata snapshot results in deletion of TC's statements"){
			val nOfPersonStats = state.maker.getStatements(jane).size
			assert(updates.forall(!_.isAssertion))
			assert(updates.size === nOfPersonStats)
		}

		it("And subsequent arrival of an unchanged TC metadata has no further effect"){
			val updates2 = state.calc.calcDiff(initSnap).toIndexedSeq
			updates2 foreach println
			assert(updates2.isEmpty)
		}
	}

	def init(initTcState: Seq[CpTcState[_ <: TC]], cpOwn: RdfMaker => Seq[Statement]): TestState = {
		val repo = Loading.emptyInMemory
		val factory = repo.getValueFactory
		val vocab = new CpVocab(factory)
		val meta = new CpmetaVocab(factory)
		val rdfMaker = new RdfMaker(vocab, meta)

		val tcGraphUri = factory.createIRI("http://test.icos.eu/tcState")
		val cpGraphUri = factory.createIRI("http://test.icos.eu/cpOwnMetaInstances")
		val tcServer = new Rdf4jInstanceServer(repo, tcGraphUri)
		val cpServer = new Rdf4jInstanceServer(repo, cpGraphUri)

		cpServer.addAll(cpOwn(rdfMaker))

		tcServer.addAll(initTcState.flatMap(getStatements(rdfMaker, _)))
		val rdfReader = new RdfReader(cpServer, tcServer)
		new TestState(new RdfDiffCalc(rdfMaker, rdfReader), rdfReader, rdfMaker, tcServer, cpServer)
	}

	def getStatements[T <: TC](rdfMaker: RdfMaker, state: CpTcState[T]): Seq[Statement] = {
		implicit val tcConf = state.tcConf
		state.stations.flatMap(rdfMaker.getStatements[T]) ++
		state.roles.flatMap(rdfMaker.getStatements[T]) ++
		state.instruments.flatMap(rdfMaker.getStatements[T])
	}

}

object RdfDiffCalcTests{
	class TestState(val calc: RdfDiffCalc, val reader: RdfReader, val maker: RdfMaker,
			val tcServer: InstanceServer, val cpServer: InstanceServer)
}
