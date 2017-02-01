package se.lu.nateko.cp.meta.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.ContentDispositionTypes
import akka.http.scaladsl.model.headers.`Content-Disposition`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import se.lu.nateko.cp.meta.InstanceServersConfig
import se.lu.nateko.cp.meta.MetaDb
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import se.lu.nateko.cp.meta.services.linkeddata.InstanceServerSerializer
import se.lu.nateko.cp.meta.services.linkeddata.UriSerializer

object LinkedDataRoute {
	private implicit val instServerMarshaller = InstanceServerSerializer.marshaller

	def apply(
		config: InstanceServersConfig,
		uriSerializer: UriSerializer,
		instanceServers: Map[String, InstanceServer]
	): Route = {

		val instServerConfs = MetaDb.getAllInstanceServerConfigs(config)
		val prefixUri = Uri(config.forDataObjects.uriPrefix.toString)
		implicit val uriMarshaller = uriSerializer.marshaller

		val genericRdfUriResourcePage: Route = extractUri{uri =>

			import akka.http.scaladsl.model.Uri.Path.{Segment, Slash}

			val scheme = uri.path match{
				case Slash(Segment("objects", _)) => "https" //objects have HTTPS URIs in our RDF
				case _ => "http"
			}

			complete(prefixUri.withPath(uri.path).withScheme(scheme))
		}

		get{
			path(("ontologies" | "resources") / Segment /){_ =>
				extractUri{uri =>
					val path = uri.path.toString

					val serverOpt: Option[(String, InstanceServer)] = instServerConfs.collectFirst{
						case (id, instServConf)
							if instServConf.writeContexts.exists(_.toString.endsWith(path)) =>
								instanceServers.get(id).map((id, _))
					}.flatten

					serverOpt match{
						case None => complete(StatusCodes.NotFound)
						case Some((id, instServer)) =>
							import ContentDispositionTypes._
							val header = `Content-Disposition`(attachment, Map("filename" -> (id + ".rdf")))
							respondWithHeader(header){ complete(instServer) }
					}
				}
			} ~
			pathPrefix("ontologies" | "resources" | "objects" | "files"){
				genericRdfUriResourcePage
			}
		}
	}
}
