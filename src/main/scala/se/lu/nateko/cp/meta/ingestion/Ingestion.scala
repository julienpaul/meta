package se.lu.nateko.cp.meta.ingestion

import org.openrdf.model.ValueFactory
import org.openrdf.model.URI
import org.openrdf.model.Statement
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import org.openrdf.repository.Repository
import scala.util.Try
import se.lu.nateko.cp.meta.utils.sesame._
import scala.util.Failure
import se.lu.nateko.cp.meta.instanceserver.SesameInstanceServer
import se.lu.nateko.cp.meta.instanceserver.RdfUpdate
import se.lu.nateko.cp.meta.AppConfig
import se.lu.nateko.cp.meta.persistence.RdfUpdateLog
import se.lu.nateko.cp.meta.instanceserver.LoggingInstanceServer
import se.lu.nateko.cp.meta.persistence.postgres.PostgresRdfLog

trait Ingester{
	def getStatements(valueFactory: ValueFactory, newUriMaker: URI => URI): Iterator[Statement]
}

object Ingestion {

	def ingestEtc(repo: Repository, conf: AppConfig): Unit = {
		val factory = repo.getValueFactory
		val ctxt = factory.createURI(conf.etcIngestionContext)
		val sesameServer = new SesameInstanceServer(repo, ctxt)
		val log = new PostgresRdfLog("etc", conf.rdfLogDbServer, conf.rdfLogDbCredentials, factory)
		sesameServer.applyAll(log.updates.toSeq)
		val instServer = new LoggingInstanceServer(sesameServer, log)
		ingest(instServer, Etc)
	}

	def ingest(target: InstanceServer, ingester: Ingester): Unit = {

		val context = target.context
		val newStatements = ingester.getStatements(target.factory, target.makeNewInstance)
		val newRepo = Loading.fromStatements(newStatements, context)
		val source = new SesameInstanceServer(newRepo, context)
		val updates = computeDiff(target, source)
		target.applyAll(updates)
		source.shutDown()
	}

	def computeDiff(from: InstanceServer, to: InstanceServer): Seq[RdfUpdate] = {
		val toRemove = to.filterNotContainedStatements(from.getStatements(None, None, None))
		val toAdd = from.filterNotContainedStatements(to.getStatements(None, None, None))

		toRemove.map(RdfUpdate(_, false)) ++ toAdd.map(RdfUpdate(_, true))
	}

}