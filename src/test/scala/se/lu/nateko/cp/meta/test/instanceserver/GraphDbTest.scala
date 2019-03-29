package se.lu.nateko.cp.meta.test.instanceserver

import com.ontotext.graphdb.example.util.EmbeddedGraphDB
import se.lu.nateko.cp.meta.utils.rdf4j._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import se.lu.nateko.cp.meta.ingestion.Ingestion
import se.lu.nateko.cp.meta.ConfigLoader
import se.lu.nateko.cp.meta.services.CpmetaVocab
import se.lu.nateko.cp.meta.instanceserver.Rdf4jInstanceServer
import se.lu.nateko.cp.meta.ingestion.Ingester
//import scala.concurrent.ExecutionContext.Implicits.global
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.ValueFactory
import scala.concurrent.ExecutionContext
import org.eclipse.rdf4j.model.impl.TreeModel
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.helpers.StatementCollector

object GraphDbTest extends App {

//	val exeServ = java.util.concurrent.Executors.newSingleThreadExecutor
//	implicit val ctxt = ExecutionContext.fromExecutorService(exeServ)

//	implicit val system = ActorSystem("graph_db_test")
//	implicit val mat = ActorMaterializer(namePrefix = Some("graph_db_test_mat"))
//	val config = ConfigLoader.default
//	implicit val _ = config.core.envriConfigs
	//import system.dispatcher

	val graph = new TreeModel();

	val rdfxmlStream = this.getClass.getResourceAsStream("/owl/cpmeta.owl")
	val rdfParser = Rio.createParser(RDFFormat.RDFXML)
	rdfParser.setRDFHandler(new StatementCollector(graph))
	rdfParser.parse(rdfxmlStream, "http://meta.icos-cp.eu/ontologies/cpmeta/")
	rdfxmlStream.close()

	val db = new EmbeddedGraphDB("/home/oleg/workspace/meta/rdfStorage")
	val repoId = "icoscp"
	if(!db.hasRepository(repoId)) db.createRepository(repoId)
	val repo = db.getRepository(repoId)
	//repo.initialize()

//	val ingester0 = Ingestion.allProviders.apply("cpMetaOnto").asInstanceOf[Ingester]
	val factory = repo.getValueFactory
	val graphUri = factory.createIRI("http://meta.icos-cp.eu/test")
//	val stats = Await.result(ingester0.getStatements(factory), Duration.Inf).toIndexedSeq
//	val ingester = new PreFetchedIngester(stats)

//	val server = new Rdf4jInstanceServer(repo, graphUri)
	//val doneFut = Ingestion.ingest(server, ingester, factory)
//	val doneFut = Future{
//
//		
//
//		def getSize = repo.accessEagerly{conn =>
//			conn.hasStatement(null, null, null, false)
//			conn.size()
//		}
//
//		getSize
//		getSize
//		val size = getSize
//		println("Size = " + size)
//
//
//	}

	val conn = repo.getConnection
	try{
		graph.forEach{stat =>
			println("Checking statement " + stat.toString)
			val res = conn.hasStatement(stat, false, graphUri)
			println("..." + res.toString)
		}
		println("all done ok!")
	}finally{
		conn.close()
		repo.shutDown()
		db.close()
	}
//	Await.ready(doneFut, Duration.Inf)
//	ctxt.shutdown()
//	exeServ.shutdown()

//	system.terminate()
}

class PreFetchedIngester(stats: IndexedSeq[Statement]) extends Ingester{
	override def getStatements(valueFactory: ValueFactory)(implicit ctxt: ExecutionContext) = Future.successful(stats.iterator)
}
