package se.lu.nateko.cp.meta.routes

import scala.concurrent.duration.DurationInt
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import se.lu.nateko.cp.meta.CpmetaJsonProtocol
import se.lu.nateko.cp.meta.FileDeletionDto
import se.lu.nateko.cp.meta.LabelingStatusUpdate
import se.lu.nateko.cp.meta.LabelingUserDto
import se.lu.nateko.cp.meta.services.labeling.StationLabelingService
import se.lu.nateko.cp.meta.services.IllegalLabelingStatusException
import se.lu.nateko.cp.meta.services.UnauthorizedStationUpdateException
import se.lu.nateko.cp.meta.services.UnauthorizedUserInfoUpdateException
import spray.json.JsObject
import java.net.URI
import akka.http.scaladsl.unmarshalling.Unmarshaller
import scala.concurrent.Future
import scala.util.Try


object LabelingApiRoute extends CpmetaJsonProtocol{

	private val exceptionHandler = ExceptionHandler{

		case authErr: UnauthorizedStationUpdateException =>
			complete((StatusCodes.Unauthorized, authErr.getMessage))

		case authErr: UnauthorizedUserInfoUpdateException =>
			complete((StatusCodes.Unauthorized, authErr.getMessage))

		case authErr: IllegalLabelingStatusException =>
			complete((StatusCodes.BadRequest, authErr.getMessage))

		case err => throw err
	}

	private implicit val urlUnmarshaller: Unmarshaller[String, URI] =
		Unmarshaller(_ => s => Future.fromTry(Try{new URI(s)}))

	def apply(
		service: StationLabelingService,
		authRouting: AuthenticationRouting
	)(implicit mat: Materializer): Route = (handleExceptions(exceptionHandler) & pathPrefix("labeling")){

		implicit val ctxt = mat.executionContext

		post {
			authRouting.mustBeLoggedIn{ uploader =>
				path("save") {
					entity(as[JsObject]){uploadMeta =>
						service.saveStationInfo(uploadMeta, uploader)
						complete(StatusCodes.OK)
					}
				} ~
				path("updatestatus"){
					entity(as[LabelingStatusUpdate]){update =>
						service.updateStatus(update.stationUri, update.newStatus, uploader).get
						complete(StatusCodes.OK)
					}
				} ~
				path("saveuserinfo") {
					entity(as[LabelingUserDto]){userInfo =>
						service.saveUserInfo(userInfo, uploader)
						complete(StatusCodes.OK)
					}
				} ~
				path("fileupload"){
					entity(as[Multipart.FormData]){ fdata =>
						onSuccess(fdata.toStrict(1.hour)){strictFormData =>
							onSuccess(service.processFile(strictFormData, uploader)){
								complete(StatusCodes.OK)
							}
						}
					}
				} ~
				path("filedeletion"){
					entity(as[FileDeletionDto]){ fileInfo =>
						service.deleteFile(fileInfo.stationUri, fileInfo.file, uploader)
						complete(StatusCodes.OK)
					}
				}
			}
		} ~
		get{
			path("userinfo"){
				authRouting.mustBeLoggedIn{ user =>
					complete(service.getLabelingUserInfo(user))
				}
			} ~
			path("filepack" / Segment){ _ =>
				parameter("stationId".as[URI]){stationId =>
					complete(service.getFilePack(stationId))
				}
			}
		}
	}

}
