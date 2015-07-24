package se.lu.nateko.cp.meta.labeler

import org.openrdf.model.URI
import org.openrdf.model.vocabulary.RDF
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import org.openrdf.model.vocabulary.OWL

object LabelerHelpers {

	def getSingleType(uri: java.net.URI, instServer: InstanceServer): URI = {

		val instUri = instServer.factory.createURI(uri.toString)
		val namedIndivid = instServer.factory.createURI(OWL.NAMESPACE, "NamedIndividual")

		val types = instServer.getValues(instUri, RDF.TYPE).collect{
			case classUri: URI if classUri != namedIndivid => classUri
		}

		assert(types.size == 1, s"Every individual is expected to have exactly one type, but $uri had ${types.size}")
		types.head
	}
}