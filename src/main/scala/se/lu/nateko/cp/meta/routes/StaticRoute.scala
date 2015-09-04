package se.lu.nateko.cp.meta.routes

import se.lu.nateko.cp.meta.CpmetaConfig
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._

object StaticRoute {

	def fromResource(path: String, mediaType: MediaType): HttpResponse = {
		val is = getClass.getResourceAsStream(path)
		val bytes = org.apache.commons.io.IOUtils.toByteArray(is)
		val contType = ContentType(mediaType, HttpCharsets.`UTF-8`)
		HttpResponse(entity = HttpEntity(contType, bytes))
	}

	def apply(config: CpmetaConfig): Route = get{
		pathPrefix("edit" / Segment){ontId =>
			if(config.onto.instOntoServers.contains(ontId)){
				pathEndOrSingleSlash{
					complete(fromResource("/www/index.html", MediaTypes.`text/html`))
				}
			} else
				complete((StatusCodes.NotFound, s"Unrecognized metadata entry project: $ontId"))
		} ~
		path("ontologies" / Segment){ ontId =>
			config.onto.instOntoServers.get(ontId) match{
				case None =>
					complete(StatusCodes.NotFound)
				case Some(ontConf) =>
					val ontId = ontConf.ontoId
					val ontRes = config.onto.ontologies.find(_.ontoId.contains(ontId)).map(_.owlResource)
					ontRes match{
						case None =>
							complete(StatusCodes.NotFound)
						case Some(owlResource) => complete(fromResource(owlResource, MediaTypes.`text/plain`))
					}
			}
		} ~
		path("bundle.js"){
			complete(fromResource("/www/bundle.js", MediaTypes.`application/javascript`))
		}
	}

}