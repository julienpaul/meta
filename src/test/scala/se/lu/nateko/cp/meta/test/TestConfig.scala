package se.lu.nateko.cp.meta.test

import org.semanticweb.owlapi.apibinding.OWLManager
import se.lu.nateko.cp.meta.utils.owlapi._
import se.lu.nateko.cp.meta.instanceserver.InstanceServer
import se.lu.nateko.cp.meta.instanceserver.Rdf4jInstanceServer
import se.lu.nateko.cp.meta.utils.rdf4j.Loading
import org.semanticweb.owlapi.model.PrefixManager
import org.semanticweb.owlapi.util.DefaultPrefixManager
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.OWLDataProperty
import org.semanticweb.owlapi.model.OWLObjectProperty
import org.eclipse.rdf4j.rio.RDFFormat

object TestConfig {
	val manager = OWLManager.createOWLOntologyManager
	val factory = manager.getOWLDataFactory
	lazy val owlOnto = {
		getOntologyFromJarResourceFile("/../classes/owl/uiannotations.owl", manager)
		getOntologyFromJarResourceFile("/../classes/owl/cpmeta.owl", manager)
		getOntologyFromJarResourceFile("/owl/cpmetaui.owl", manager)
	}

	val instOntUri = "http://meta.icos-cp.eu/resources/cpmeta/"
	val ontUri = "http://meta.icos-cp.eu/ontologies/cpmeta/"

	lazy val instServer: InstanceServer = {
		val repo = Loading.fromResource("/owl/cpmetainstances.owl", instOntUri)
		val factory = repo.getValueFactory
		val instOnt = factory.createIRI(instOntUri)
		val ont = factory.createIRI(ontUri)
		Loading.loadResource(repo, "/../classes/owl/cpmeta.owl", ontUri, RDFFormat.RDFXML)
		new Rdf4jInstanceServer(repo, Seq(ont, instOnt), Seq(instOnt))
	}

	private val prefixManager: PrefixManager =
		new DefaultPrefixManager(null, null, ontUri)

	def getOWLClass(localName: String): OWLClass =
		factory.getOWLClass(localName, prefixManager)

	def getDataProperty(localName: String): OWLDataProperty =
		factory.getOWLDataProperty(localName, prefixManager)

	def getObjectProperty(localName: String): OWLObjectProperty =
		factory.getOWLObjectProperty(localName, prefixManager)
}