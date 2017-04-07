package se.lu.nateko.cp.meta.ingestion

import scala.io.Source

import org.openrdf.model.Statement
import org.openrdf.model.URI
import org.openrdf.model.Value
import org.openrdf.model.ValueFactory
import org.openrdf.model.vocabulary.RDF
import org.openrdf.model.vocabulary.RDFS

import se.lu.nateko.cp.meta.services.CpVocab
import se.lu.nateko.cp.meta.services.CpmetaVocab
import se.lu.nateko.cp.meta.utils.sesame._

class PeopleAndOrgsIngester(pathToTextRes: String) extends Ingester{

	override def isAppendOnly = true

	private val regexp = """^(.+),\ (.+):\ (.+)\ \((.+)\)$""".r

	def getStatements(factory: ValueFactory): Iterator[Statement] = {

		implicit val f = factory
		val vocab = new CpVocab(factory)
		val metaVocab = new CpmetaVocab(factory)
		val roleId = "Researcher"
		val role = vocab.getRole(roleId)

		val info = Source
			.fromInputStream(getClass.getResourceAsStream(pathToTextRes), "UTF-8")
			.getLines.map(_.trim).filter(!_.isEmpty).map{
				case regexp(lname, fname, orgName, orgId) => (lname, fname, orgName, orgId)
			}.toIndexedSeq

		val orgTriples = info.collect{
			case (_, _, orgName, orgId) => (orgName, orgId)
		}.distinct.flatMap{
			case (orgName, orgId) =>
				val org = vocab.getOrganization(orgId)
				Seq[(URI, URI, Value)](
					(org, RDF.TYPE, metaVocab.orgClass),
					(org, metaVocab.hasName, orgName),
					(org, RDFS.LABEL, orgId)
				)
		}

		val personTriples = info.collect{
			case (lname, fname, _, _) => (lname, fname)
		}.distinct.flatMap{
			case (lname, fname) =>
				val person = vocab.getPerson(fname, lname)
				Seq[(URI, URI, Value)](
					(person, RDF.TYPE, metaVocab.personClass),
					(person, metaVocab.hasFirstName, fname),
					(person, metaVocab.hasLastName, lname)
				)
		}

		val membershipTriples = info.flatMap{
			case (lname, fname, orgName, orgId) =>
				val org = vocab.getOrganization(orgId)
				val person = vocab.getPerson(fname, lname)
				val membership = vocab.getMembership(orgId, roleId, lname)
				Seq[(URI, URI, Value)](
					(person, metaVocab.hasMembership, membership),
					(membership, RDF.TYPE, metaVocab.membershipClass),
					(membership, RDFS.LABEL, s"$lname as $roleId at $orgId"),
					(membership, metaVocab.hasRole, role),
					(membership, metaVocab.atOrganization, org)
				)
		}
		(orgTriples ++ personTriples ++ membershipTriples).map(factory.tripleToStatement).iterator
	}
}