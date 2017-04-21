package se.lu.nateko.cp.meta.services.upload

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.openrdf.model.Resource
import org.openrdf.model.Statement
import org.openrdf.model.URI

import se.lu.nateko.cp.meta.api.SparqlQuery
import se.lu.nateko.cp.meta.api.SparqlRunner
import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import se.lu.nateko.cp.meta.instanceserver.RdfUpdate
import se.lu.nateko.cp.meta.services.CpVocab
import se.lu.nateko.cp.meta.services.CpmetaVocab
import se.lu.nateko.cp.meta.utils.sesame._

class MetadataUpdater(vocab: CpVocab, metaVocab: CpmetaVocab, sparql: SparqlRunner) {
	import MetadataUpdater._
	import StatementStability._

	def calculateUpdates(hash: Sha256Sum, oldStatements: Seq[Statement], newStatements: Seq[Statement]): Seq[RdfUpdate] = {
		if(oldStatements.isEmpty) newStatements.map(RdfUpdate(_, true))
		else {

			def isProvTime(pred: URI): Boolean = pred === metaVocab.prov.endedAtTime || pred === metaVocab.prov.startedAtTime
	
			val acq = vocab.getAcquisition(hash)
			val subm = vocab.getSubmission(hash)
	
			def stability(sp: SubjPred): StatementStability = {
				val (subj, pred) = sp
				if(subj == acq && isProvTime(pred)) Sticky
				else if(subj == subm && isProvTime(pred)) Fixed
				else Plain
			}
	
			val oldBySp = new BySubjPred(oldStatements)
			val newBySp = new BySubjPred(newStatements)
			val allSps = (oldBySp.sps ++ newBySp.sps).toSeq

			allSps.flatMap(sp => stability(sp) match {
				case Fixed =>
					if(oldBySp(sp).isEmpty) newBySp(sp).map(RdfUpdate(_, true))
					else Nil

				case Sticky if(newBySp(sp).isEmpty) =>
					Nil

				case _ =>
					diff(oldBySp(sp), newBySp(sp))
			})
		}
	}

	def getCurrentStatements(hash: Sha256Sum, server: InstanceServer)(implicit ctxt: ExecutionContext): Future[Seq[Statement]] = {
		val objUri = vocab.getDataObject(hash)
		if(!server.hasStatement(Some(objUri), None, None)) Future.successful(Nil)
		else {
			val fromClauses = server.writeContexts.map(graph => s"FROM <$graph>").mkString("\n")
			val query = SparqlQuery(s"""construct{?s ?p ?o}
				|$fromClauses
				|where{
				|	{
				|		BIND(<$objUri> AS ?s)
				|		?s ?p ?o
				|	} UNION
				|	{
				|		<$objUri> ?p0 ?s .
				|		?s ?p ?o
				|	}
				|}""".stripMargin)
			sparql.evaluateGraphQuery(query).map(_.toIndexedSeq)
		}
	}

}

object MetadataUpdater{

	private object StatementStability extends Enumeration{
		type StatementStability = Value
		val Plain, Sticky, Fixed = Value
	}

	private type SubjPred = (Resource, URI)


	def diff(olds: Seq[Statement], news: Seq[Statement]): Seq[RdfUpdate] =
		olds.diff(news).map(RdfUpdate(_, false)) ++
		news.diff(olds).map(RdfUpdate(_, true))

	private class BySubjPred(stats: Seq[Statement]){

		private val bySp = stats.groupBy(s => (s.getSubject, s.getPredicate))

		def apply(sp: SubjPred): Seq[Statement] = bySp.getOrElse(sp, Nil)

		def sps: Set[SubjPred] = bySp.keySet
	}

}
