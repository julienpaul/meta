package se.lu.nateko.cp.meta.services.labeling

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import org.eclipse.rdf4j.model.IRI
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.meta.utils.rdf4j._
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.HttpEntity.Chunked
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.HttpResponse
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import se.lu.nateko.cp.meta.core.crypto.Sha256Sum


trait FileService { self: StationLabelingService =>

	private val (factory, vocab) = getFactoryAndVocab(server)

	def processFile(upload: Multipart.FormData.Strict, uploader: UserId)(implicit ex: ExecutionContext): Future[Unit] = Future{
		val nameToParts = upload.strictParts.map(part => (part.name, part)).toMap
		val fileType = nameToParts("fileType").entity.data.decodeString("UTF-8")
		val stationUri = nameToParts("stationUri").entity.data.decodeString("UTF-8")
		val filePart = nameToParts("uploadedFile")
		val fileName = filePart.filename.get
		val fileContent = filePart.entity.data

		val station = factory.createIRI(stationUri)

		assertThatWriteIsAuthorized(station, uploader)

		val stationUriBytes = stationUri.getBytes(StandardCharsets.UTF_8)
		val hash = fileStorage.saveAsFile(fileContent, Some(stationUriBytes))
		val file = vocab.files.getUri(hash)

		val newInfo = Seq(
			(station, vocab.hasAssociatedFile, file),
			(file, vocab.files.hasName, vocab.lit(fileName)),
			(file, vocab.files.hasType, vocab.lit(fileType))
		).map{
			case (subj, pred, obj) => factory.createStatement(subj, pred, obj)
		}

		val currentInfo = (server.getStatements(Some(station), Some(vocab.hasAssociatedFile), Some(file)) ++
			server.getStatements(Some(file), Some(vocab.files.hasName), None) ++
			server.getStatements(Some(file), Some(vocab.files.hasType), None)).toIndexedSeq

		server.applyDiff(currentInfo, newInfo)
	}

	def deleteFile(station: java.net.URI, file: java.net.URI, uploader: UserId): Unit = {
		val stationUri: IRI = factory.createIRI(station.toString)

		assertThatWriteIsAuthorized(stationUri, uploader)

		val fileUri = factory.createIRI(file)
		server.remove(factory.createStatement(stationUri, vocab.hasAssociatedFile, fileUri))
	}

	def getFilePack(stationId: java.net.URI): HttpResponse = {
		val stationUri: IRI = factory.createIRI(stationId.toString)
		val fileHashesAndNames: Seq[(Sha256Sum, String)] = server
			.getUriValues(stationUri, vocab.hasAssociatedFile)
			.map{fileUri =>
				val hash = vocab.files.getFileHash(fileUri)
				val fileName = server.getStringValues(fileUri, vocab.files.hasName, InstanceServer.ExactlyOne).head
				(hash, fileName)
			}
		val source = fileStorage.getZipSource(fileHashesAndNames)
		val entity = Chunked.fromData(MediaTypes.`application/zip`, source)
		HttpResponse(entity = entity)
	}

}
