package se.lu.nateko.cp.meta.onto.labeler

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.vocabulary.RDFS
import se.lu.nateko.cp.meta.ResourceDto
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import org.semanticweb.owlapi.model.OWLEntity
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.search.EntitySearcher
import se.lu.nateko.cp.meta.utils.owlapi._
import org.semanticweb.owlapi.model.OWLAnnotationProperty
import scala.jdk.CollectionConverters.IteratorHasAsScala

trait InstanceLabeler {

	// rdfs:label is the default, to be overridden in some implementations
	def getLabel(instUri: IRI, instServer: InstanceServer): String =
		getRdfsLabel(instUri, instServer).getOrElse(instUri.getLocalName)

	final def getRdfsLabel(instUri: IRI, instServer: InstanceServer): Option[String] =
		instServer.getValues(instUri, RDFS.LABEL).headOption.map(_.stringValue)

	final def getRdfsComment(instUri: IRI, instServer: InstanceServer): Option[String] =
		instServer.getValues(instUri, RDFS.COMMENT).headOption.map(_.stringValue)

	final def getInfo(instUri: IRI, instServer: InstanceServer) = ResourceDto(
		displayName = getLabel(instUri, instServer),
		uri = java.net.URI.create(instUri.stringValue),
		comment = getRdfsComment(instUri, instServer)
	)
}

object Labeler{

	object rdfs extends InstanceLabeler

	def joinMultiValues(values: Iterable[String]): String = values.toList.distinct match{
		case only :: Nil => only
		case Nil => ""
		case list => list.mkString("{", ",", "}")
	}

	def joinComponents(values: Iterable[String]): String = values.mkString("")

	def getLabel(entity: OWLEntity, onto: OWLOntology): String =
		getRdfsLabel(entity, onto).getOrElse(getLastFragment(entity.getIRI))

	def getRdfsLabel(entity: OWLEntity, onto: OWLOntology): Option[String] =
		getAnnotation(entity, getFactory(onto).getRDFSLabel, onto)

	def getRdfsComment(entity: OWLEntity, onto: OWLOntology): Option[String] =
		getAnnotation(entity, getFactory(onto).getRDFSComment, onto)

	def getInfo(entity: OWLEntity, onto: OWLOntology) = ResourceDto(
		displayName = getLabel(entity, onto),
		uri = entity.getIRI.toURI,
		comment = getRdfsComment(entity, onto)
	)

	private def getAnnotation(entity: OWLEntity, anno: OWLAnnotationProperty, onto: OWLOntology): Option[String] = EntitySearcher
		.getAnnotations(entity, onto.importsClosure, anno)
		.iterator.asScala
		.map(_.getValue.asLiteral.toOption)
		.collect{case Some(lit) => lit.getLiteral}
		.toSeq
		.headOption

	private def getFactory(onto: OWLOntology) = onto.getOWLOntologyManager.getOWLDataFactory
}

