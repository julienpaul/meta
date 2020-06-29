package se.lu.nateko.cp.meta.upload.subforms

import scala.concurrent.Future
import scala.util.{Try, Success, Failure}

import org.scalajs.dom

import se.lu.nateko.cp.doi.Doi

import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import se.lu.nateko.cp.meta.core.data.Envri
import se.lu.nateko.cp.meta.core.data.OptionalOneOrSeq
import se.lu.nateko.cp.meta.SubmitterProfile
import se.lu.nateko.cp.meta.upload._

import formcomponents._
import ItemTypeRadio.{ItemType, Collection}
import UploadApp.whenDone
import Utils._


class AboutPanel(subms: IndexedSeq[SubmitterProfile])(implicit bus: PubSubBus, envri: Envri.Envri) {

	def submitter = submitterIdSelect.value.withMissingError("Submitter Id not set")
	def isInNewItemMode: Boolean = newUpdateControl.value.contains("new")
	def itemType: Option[ItemType] = typeControl.value
	def file: Try[dom.File] = fileInput.file
	def itemName: Try[String] = if(isInNewItemMode) fileInput.file.map(_.name) else fileNameText.value;
	def itemHash: Try[Sha256Sum] = if(isInNewItemMode) fileInput.hash else Success(fileHash.get)
	def previousVersion: Try[OptionalOneOrSeq[Sha256Sum]] = previousVersionInput.value.withErrorContext("Previous version")
	def existingDoi: Try[Option[Doi]] = existingDoiInput.value.withErrorContext("Pre-existing DOI")

	def refreshFileHash(): Future[Unit] = if (fileInput.hasBeenModified) fileInput.rehash() else Future.successful(())

	private val newUpdateControl = new Radio[String]("new-update-radio", onNewUpdateSelected, s => Some(s), s => s)
	private val submitterIdSelect = new Select[SubmitterProfile]("submitteridselect", _.id, autoselect = true, onSubmitterSelected)
	private val typeControl = new ItemTypeRadio("file-type-radio", onItemTypeSelected)
	private val fileElement = new HtmlElements("#file-element")
	private val fileInputElement = new HtmlElements("#fileinput")
	private val filenameElement = new HtmlElements("#filename")
	private val fileInput = new FileInput("fileinput", updateForm)
	private val fileNameText = new TextInput("filename", updateForm)
	private val previousVersionInput = new HashOptInput("previoushash", updateForm)
	private val existingDoiInput = new DoiOptInput("existingdoi", updateForm)
	private val metadataUrlElement = new HtmlElements("#metadata-url")
	private val metadataUriInput = new UriInput("metadata-update", updateGetMetadataButton)
	private val getMetadataButton = new Button("get-metadata", getMetadata)

	private var fileHash: Option[Sha256Sum] = None

	submitterIdSelect.setOptions(subms)

	private def updateForm(): Unit = bus.publish(FormInputUpdated)

	private def onNewUpdateSelected(modeName: String): Unit = modeName match {
		case "new" =>
			bus.publish(NewItemMode)
			fileInputElement.show()
			filenameElement.hide()
			metadataUrlElement.hide()
			typeControl.enable()
		case "update" =>
			bus.publish(UpdateMetaMode)
			fileInputElement.hide()
			filenameElement.show()
			metadataUrlElement.show()
			typeControl.disable()
	}

	
	private def onItemTypeSelected(itemType: ItemType): Unit = {
		itemType match {
			case Collection =>
				fileElement.hide()
			case _ =>
				fileElement.show()
		}
		bus.publish(ItemTypeSelected(itemType))
	}

	private def onSubmitterSelected(): Unit = submitterIdSelect.value.foreach{subm =>
		bus.publish(GotStationsList(IndexedSeq.empty))
		updateGetMetadataButton()
		whenDone(Backend.stationInfo(subm.producingOrganizationClass, subm.producingOrganization)){
			stations => bus.publish(GotStationsList(stations))
		}
	}

	private def updateGetMetadataButton(): Unit = {
		val ok = for(
			_ <- submitterIdSelect.value.withMissingError("Submitter Id not set");
			_ <- metadataUriInput.value
		) yield ()

		ok match {
			case Success(_) => getMetadataButton.enable()
			case Failure(err) => getMetadataButton.disable(err.getMessage)
		}
	}
}
