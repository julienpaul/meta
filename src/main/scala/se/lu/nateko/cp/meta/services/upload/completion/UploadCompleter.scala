package se.lu.nateko.cp.meta.services.upload.completion

import java.time.Instant

import scala.concurrent.Future
import scala.util.Try

import org.eclipse.rdf4j.model.IRI

import akka.actor.ActorSystem
import se.lu.nateko.cp.meta.api.EpicPidClient
import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import se.lu.nateko.cp.meta.core.data.Envri.Envri
import se.lu.nateko.cp.meta.core.data.UploadCompletionInfo
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import se.lu.nateko.cp.meta.instanceserver.RdfUpdate
import se.lu.nateko.cp.meta.services.upload.DataObjectInstanceServers
import se.lu.nateko.cp.meta.utils.rdf4j._


class UploadCompleter(servers: DataObjectInstanceServers, epic: EpicPidClient)(implicit system: ActorSystem) {
	import system.dispatcher
	import servers.{ metaVocab, vocab }

	def completeUpload(hash: Sha256Sum, info: UploadCompletionInfo)(implicit envri: Envri): Future[Report] = {
		for(
			(specific, server) <- Future.fromTry(getSpecificCompleter(hash));
			specificUpdates <- specific.getUpdates(hash, info);
			stopTimeUpdates = getUploadStopTimeUpdates(server, hash);
			byteSizeUpdates = getBytesSizeUpdates(server, hash, info.bytes);
			report <- specific.finalize(hash);
			_ <- Future.fromTry(server.applyAll(specificUpdates ++ stopTimeUpdates ++ byteSizeUpdates))
		) yield report
	}

	private def getSpecificCompleter(hash: Sha256Sum)(implicit envri: Envri): Try[(FormatSpecificCompleter, InstanceServer)] =
		for(
			objSpec <- servers.getDataObjSpecification(hash);
			format <- servers.getObjSpecificationFormat(objSpec);
			server <- servers.getInstServerForFormat(format)
		) yield (getSpecificCompleter(server, format), server)


	private def getSpecificCompleter(server: InstanceServer, format: IRI)(implicit envri: Envri): FormatSpecificCompleter = {
		if(format === metaVocab.wdcggFormat)
			new WdcggUploadCompleter(server, vocab, metaVocab)
		else if(format === metaVocab.etcFormat || format === metaVocab.socatFormat)
			new TimeSeriesUploadCompleter(server, epic, vocab, metaVocab)
		else new EpicPidMinter(epic, vocab)
	}

	private def getUploadStopTimeUpdates(server: InstanceServer, hash: Sha256Sum)(implicit envri: Envri): Seq[RdfUpdate] = {
		val submissionUri = vocab.getSubmission(hash)
		if(server.hasStatement(Some(submissionUri), Some(metaVocab.prov.endedAtTime), None)) Nil
		else {
			val stopInfo = vocab.factory.createStatement(submissionUri, metaVocab.prov.endedAtTime, vocab.lit(Instant.now))
			Seq(RdfUpdate(stopInfo, true))
		}
	}

	private def getBytesSizeUpdates(server: InstanceServer, hash: Sha256Sum, size: Long)(implicit envri: Envri): Seq[RdfUpdate] = {
		val dobj = vocab.getDataObject(hash)
		if(server.hasStatement(Some(dobj), Some(metaVocab.hasSizeInBytes), None)) Nil //byte size cannot change for same hash
		else{
			val sizeInfo = vocab.factory.createStatement(dobj, metaVocab.hasSizeInBytes, vocab.lit(size))
			Seq(RdfUpdate(sizeInfo, true))
		}
	}

}
