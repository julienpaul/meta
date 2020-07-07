package se.lu.nateko.cp.meta.upload.subforms


import scala.util.{Try, Success, Failure}

import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import se.lu.nateko.cp.meta.core.data.Envri
import se.lu.nateko.cp.meta.core.data.OptionalOneOrSeq
import se.lu.nateko.cp.meta.SubmitterProfile
import se.lu.nateko.cp.meta.upload._
import se.lu.nateko.cp.meta.{UploadDto, DataObjectDto, DataProductionDto, ElaboratedProductMetadata}

import formcomponents._
import ItemTypeRadio.{ItemType, Collection, Data, Document}
import UploadApp.whenDone
import Utils._
import se.lu.nateko.cp.meta.core.data.TemporalCoverage
import se.lu.nateko.cp.meta.core.data.LatLonBox
import java.net.URI
import se.lu.nateko.cp.meta.core.data.Position

class L3Panel(covs: IndexedSeq[SpatialCoverage])(implicit bus: PubSubBus, envri: Envri.Envri) extends PanelSubform(".l3-section"){

	def meta(productionDto: => Try[DataProductionDto]): Try[ElaboratedProductMetadata] = for(
		title <- titleInput.value;
		descr <- descriptionInput.value;
		spatCov <- spatialCoverage;
		tempCovOpt <- timeIntevalInput.value;
		tempCov <- tempCovOpt.withMissingError("time interval");
		tempRes <- temporalResInput.value;
		prod <- productionDto;
		customLanding <- externalPageInput.value;
		varInfo <- varInfoForm.varInfos
	) yield ElaboratedProductMetadata(
		title = title,
		description = descr,
		spatial = spatCov,
		temporal = TemporalCoverage(tempCov, tempRes),
		production = prod,
		customLandingPage = customLanding,
		variables = varInfo
	)

	def spatialCoverage: Try[Either[LatLonBox, URI]] = spatialCovSelect
		.value.withMissingError("spatial coverage").flatMap{spCov =>
			if(spCov == customSpatCov) {
				for(
					minLat <- minLatInput.value;
					minLon <- minLonInput.value;
					maxLat <- maxLatInput.value;
					maxLon <- maxLonInput.value
				) yield Left(LatLonBox(Position(minLat, minLon, None), Position(maxLat, maxLon, None), None))
			} else Success(Right(spCov.uri))
		}
	private val spatCoverElements = new HtmlElements(".l3spatcover-element")

	private val titleInput = new TextInput("l3title", notifyUpdate, "elaborated product title")
	private val descriptionInput = new TextOptInput("l3descr", notifyUpdate)
	private val timeStartInput = new InstantInput("l3startinput", notifyUpdate)
	private val timeStopInput = new InstantInput("l3stopinput", notifyUpdate)
	private val timeIntevalInput = new TimeIntevalInput(timeStartInput, timeStopInput)
	private val temporalResInput = new TextOptInput("l3tempres", notifyUpdate)
	private val spatialCovSelect = new Select[SpatialCoverage]("l3spatcoverselect", _.label, autoselect = false, onSpatCoverSelected)
	private val varInfoForm = new L3VarInfoForm("l3varinfo-form", notifyUpdate)
	private val externalPageInput = new UriOptInput("l3landingpage", notifyUpdate)

	private val minLatInput = new DoubleInput("l3minlat", notifyUpdate)
	private val minLonInput = new DoubleInput("l3minlon", notifyUpdate)
	private val maxLatInput = new DoubleInput("l3maxlat", notifyUpdate)
	private val maxLonInput = new DoubleInput("l3maxlon", notifyUpdate)

	private val customSpatCov = new SpatialCoverage(null, "Custom spatial coverage")

	spatialCovSelect.setOptions(customSpatCov +: covs)

	def resetForm(): Unit = {
		Iterable(
			titleInput, descriptionInput, timeStartInput, timeStopInput,
			temporalResInput, externalPageInput, minLatInput, minLonInput,
			maxLatInput, maxLonInput
		).foreach(_.reset())
	}

	bus.subscribe{
		case GotUploadDto(upDto) => handleDto(upDto)
		case ObjSpecSelected(spec) => onLevelSelected(spec.dataLevel)
		case LevelSelected(level) => onLevelSelected(level)
	}

	private def onLevelSelected(level: Int): Unit = if(level == 3) show() else hide()

	private def onSpatCoverSelected(): Unit = {
		if(spatialCovSelect.value == Some(customSpatCov)) spatCoverElements.show()
		else spatCoverElements.hide()
	}

	private def handleDto(upDto: UploadDto): Unit = upDto match {
		case dto: DataObjectDto => dto.specificInfo match{
			case Left(l3) =>
				titleInput.value = l3.title
				descriptionInput.value = l3.description
				timeStartInput.value = l3.temporal.interval.start
				timeStopInput.value = l3.temporal.interval.stop
				temporalResInput.value = l3.temporal.resolution
				externalPageInput.value = l3.customLandingPage
				varInfoForm.setValues(l3.variables)
				l3.spatial match{
					case Left(box) =>
						minLatInput.value = box.min.lat
						minLonInput.value = box.min.lon
						maxLatInput.value = box.max.lat
						maxLonInput.value = box.max.lon
						spatialCovSelect.value = customSpatCov
					case Right(covUri) =>
						minLatInput.reset()
						minLonInput.reset()
						maxLatInput.reset()
						maxLonInput.reset()
						covs.find(_.uri == covUri).fold(spatialCovSelect.reset()){
							cov => spatialCovSelect.value = cov
						}
				}
				show()
			case _ =>
				hide()
		}
		case _ =>
			hide()
	}
}
