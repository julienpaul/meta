package se.lu.nateko.cp.meta.upload

import java.net.URI

import se.lu.nateko.cp.meta.core.data.Envri
import se.lu.nateko.cp.meta.core.data.Envri.Envri

object SparqlQueries {

	type Binding = Map[String, String]

	private def sitesStations(orgFilter: String) = s"""PREFIX cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|PREFIX sitesmeta: <https://meta.fieldsites.se/ontologies/sites/>
		|SELECT *
		|FROM <https://meta.fieldsites.se/resources/sites/>
		|WHERE { ?station a sitesmeta:Station ; cpmeta:hasName ?name; cpmeta:hasStationId ?id .
		| $orgFilter }
		|order by ?name""".stripMargin

	private def icosStations(orgFilter: String) = s"""PREFIX cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|SELECT *
		|FROM <http://meta.icos-cp.eu/resources/icos/>
		|FROM <http://meta.icos-cp.eu/resources/cpmeta/>
		|FROM <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|FROM <http://meta.icos-cp.eu/resources/extrastations/>
		|WHERE {
		| ?station cpmeta:hasName ?name; cpmeta:hasStationId ?id .
		| $orgFilter }
		|order by ?name""".stripMargin

	def stations(orgClass: Option[URI], producingOrg: Option[URI])(implicit envri: Envri): String = {
		val orgClassFilter = orgClass.map(org => s"?station a/rdfs:subClassOf* <$org> .")
		val producingOrgFilter: Option[String] = producingOrg.map(org => s"FILTER(?station = <$org>) .")
		val orgFilter = Iterable(orgClassFilter, producingOrgFilter).flatten.mkString("\n")
		envri match {
			case Envri.SITES => sitesStations(orgFilter)
			case Envri.ICOS => icosStations(orgFilter)
		}
	}

	def toStation(b: Binding) = Station(new URI(b("station")), b("id"), b("name"))

	def sites(station: URI): String = s"""PREFIX cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|SELECT ?site ?name
		|WHERE {
		|	<$station> cpmeta:operatesOn ?site .
		|	?site rdfs:label ?name }
		|order by ?name""".stripMargin

	def toSite(b: Binding) = Site(new URI(b("site")), b("name"))

	def samplingpoints(site: URI): String = s"""PREFIX cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|SELECT *
		|WHERE {
		|	<$site> cpmeta:hasSamplingPoint ?point .
		|	?point rdfs:label ?name .
		|	?point cpmeta:hasLatitude ?latitude .
		|	?point cpmeta:hasLongitude ?longitude }
		|order by ?name""".stripMargin

	def toSamplingPoint(b: Binding) = SamplingPoint(new URI(b("point")), b("latitude").toDouble, b("longitude").toDouble, b("name"))

	private def objSpecsTempl(from: String) = s"""PREFIX cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|SELECT *
		|FROM <${from}>
		|WHERE {
		|	?spec cpmeta:hasDataLevel ?dataLevel ; rdfs:label ?name .
		|	OPTIONAL{?spec cpmeta:containsDataset ?dataset}
		|} order by ?name""".stripMargin

	def objSpecs(implicit envri: Envri): String = envri match {
		case Envri.SITES => objSpecsTempl("https://meta.fieldsites.se/resources/sites/")
		case Envri.ICOS => objSpecsTempl("http://meta.icos-cp.eu/resources/cpmeta/")
	}

	def toObjSpec(b: Binding) = ObjSpec(new URI(b("spec")), b("name"), b("dataLevel").toInt, b.contains("dataset"))

	//Only for ICOS for now
	def l3spatialCoverages = """|prefix cpmeta: <http://meta.icos-cp.eu/ontologies/cpmeta/>
		|select *
		|from <http://meta.icos-cp.eu/resources/cpmeta/>
		|where{
		|	{{?cov a cpmeta:SpatialCoverage } union {?cov a cpmeta:LatLonBox}}
		|	?cov rdfs:label ?label
		|}
		|""".stripMargin

	def toSpatialCoverage(b: Binding) = new SpatialCoverage(new URI(b("cov")), b("label"))
}
