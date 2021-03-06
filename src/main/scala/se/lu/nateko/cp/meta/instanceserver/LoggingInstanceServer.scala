package se.lu.nateko.cp.meta.instanceserver

import org.eclipse.rdf4j.model.IRI
import se.lu.nateko.cp.meta.persistence.RdfUpdateLog
import scala.util.Try
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.Statement

class LoggingInstanceServer(inner: InstanceServer, log: RdfUpdateLog) extends InstanceServer{

	def factory = inner.factory
	def readContexts = inner.readContexts
	def writeContexts = inner.writeContexts
	def makeNewInstance(prefix: IRI) = inner.makeNewInstance(prefix)

	def getStatements(subject: Option[IRI], predicate: Option[IRI], obj: Option[Value]) =
		inner.getStatements(subject, predicate, obj)

	def hasStatement(subject: Option[IRI], predicate: Option[IRI], obj: Option[Value]): Boolean =
		inner.hasStatement(subject, predicate, obj)

	def filterNotContainedStatements(statements: IterableOnce[Statement]): Seq[Statement] =
		inner.filterNotContainedStatements(statements)

	def applyAll(updates: Seq[RdfUpdate]): Try[Unit] = {
		log.appendAll(updates)
		inner.applyAll(updates)
	}

	override def shutDown(): Unit = {
		inner.shutDown()
		log.close()
	}

	def writeContextsView = new LoggingInstanceServer(inner.writeContextsView, log)
}