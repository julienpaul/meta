package se.lu.nateko.cp.meta.services.upload

import akka.http.scaladsl.marshalling.{Marshaller, Marshalling, ToResponseMarshaller, ToEntityMarshaller}
import akka.http.scaladsl.marshalling.Marshalling._
import akka.http.scaladsl.model._
import se.lu.nateko.cp.meta.api.Doi
import se.lu.nateko.cp.meta.core.data.{DataObject, EnvriConfig, StaticCollection}
import se.lu.nateko.cp.meta.core.data.JsonSupport._
import se.lu.nateko.cp.meta.services.CpVocab

import scala.concurrent.Future
import spray.json._
import play.twirl.api.Html
import java.net.URI

import se.lu.nateko.cp.meta.core.data.Envri.Envri
import se.lu.nateko.cp.meta.api.CitationClient

class PageContentMarshalling(handleService: URI, citer: CitationClient, vocab: CpVocab) {

	implicit def dataObjectMarshaller(implicit envri: Envri): ToResponseMarshaller[() => Option[DataObject]] =
		makeMarshaller(views.html.LandingPage(_, _, handleService, vocab), _.doi)

	implicit def statCollMarshaller(implicit envri: Envri, conf: EnvriConfig): ToResponseMarshaller[() => Option[StaticCollection]] =
		makeMarshaller(views.html.CollectionLandingPage(_, _), _.doi)

	private def makeMarshaller[T: JsonWriter](
		template: (T, Option[String]) => Html,
		toDoi: T => Option[String]
	): ToResponseMarshaller[() => Option[T]] = Marshaller{ implicit exeCtxt => producer =>

		Future(producer()).flatMap{dataItemOpt =>

			val doiOpt: Option[Doi] = dataItemOpt.flatMap(toDoi).flatMap(Doi.unapply)

			val citationOptFut: Future[Option[String]] = doiOpt match{
				case None => Future.successful(None)
				case Some(doi) => citer.getCitation(doi).map(Some(_)).recover{
					case err: Throwable =>
						Some("Error fetching the citation from DataCite: " + err.getMessage)
				}
			}

			citationOptFut.map{citOpt =>
				WithOpenCharset(MediaTypes.`text/html`, getHtml[T](dataItemOpt, template(_, citOpt), _)) ::
				WithFixedContentType(ContentTypes.`application/json`, () => PageContentMarshalling.getJson(dataItemOpt)) :: Nil
			}
		}
	}

	private def getHtml[T](dataItemOpt: Option[T], template: T => Html, charset: HttpCharset) =
		dataItemOpt match {
			case Some(obj) => HttpResponse(
				entity = HttpEntity(
					ContentType.WithCharset(MediaTypes.`text/html`, charset),
					template(obj).body
				)
			)
			case None => HttpResponse(StatusCodes.NotFound)
		}

}

object PageContentMarshalling{

	implicit val twirlHtmlEntityMarshaller: ToEntityMarshaller[Html] = Marshaller(
		_ => html => Future.successful(
			WithOpenCharset(MediaTypes.`text/html`, getHtml(html, _)) :: Nil
		)
	)
	val twirlHtmlMarshaller = implicitly[ToResponseMarshaller[Html]]

	def notFoundMarshalling(implicit envri: Envri): Future[List[Marshalling[HttpResponse]]] = {
		Future.successful {
			WithFixedContentType(
				ContentTypes.`text/html(UTF-8)`,
				() =>
					HttpResponse(
						StatusCodes.NotFound,
						entity = HttpEntity(
							ContentType.WithCharset(MediaTypes.`text/html`, HttpCharsets.`UTF-8`),
							views.html.MessagePage("Page not found", "").body
						)
					)
			) :: Nil
		}
	}

	private def getHtml(html: Html, charset: HttpCharset) = HttpEntity(
		ContentType.WithCharset(MediaTypes.`text/html`, charset),
		html.body
	)

	def getJson[T: JsonWriter](dataItemOpt: Option[T]) =
		dataItemOpt match {
			case Some(obj) => HttpResponse(
				entity = HttpEntity(ContentTypes.`application/json`, obj.toJson.prettyPrint)
			)
			case None => HttpResponse(StatusCodes.NotFound)
		}
}
