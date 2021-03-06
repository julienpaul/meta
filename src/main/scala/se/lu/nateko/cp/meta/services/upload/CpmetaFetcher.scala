package se.lu.nateko.cp.meta.services.upload

import java.net.URI

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.RDFS
import se.lu.nateko.cp.meta.core.data._
import se.lu.nateko.cp.meta.instanceserver.FetchingHelper
import se.lu.nateko.cp.meta.services.CpmetaVocab
import se.lu.nateko.cp.meta.utils.rdf4j._
import se.lu.nateko.cp.meta.utils.parseJsonStringArray
import se.lu.nateko.cp.meta.utils.parseCommaSepList

import scala.util.Try

trait CpmetaFetcher extends FetchingHelper{
	protected final lazy val metaVocab = new CpmetaVocab(server.factory)

	def getSpecification(spec: IRI, fetcher: PlainStaticObjectFetcher) = DataObjectSpec(
		self = getLabeledResource(spec),
		project = getProject(getSingleUri(spec, metaVocab.hasAssociatedProject)),
		theme = getDataTheme(getSingleUri(spec, metaVocab.hasDataTheme)),
		format = getLabeledResource(spec, metaVocab.hasFormat),
		encoding = getLabeledResource(spec, metaVocab.hasEncoding),
		dataLevel = getSingleInt(spec, metaVocab.hasDataLevel),
		datasetSpec = getOptionalUri(spec, metaVocab.containsDataset).map(getLabeledResource),
		documentation = server.getUriValues(spec, metaVocab.hasDocumentationObject).map(fetcher.getPlainStaticObject),
		keywords = getOptionalString(spec, metaVocab.hasKeywords).map(s => parseCommaSepList(s).toIndexedSeq)
	)

	def getOptionalSpecificationFormat(spec: IRI): Option[IRI] = getOptionalUri(spec, metaVocab.hasFormat)

	protected def getPosition(point: IRI) = Position(
		lat = getSingleDouble(point, metaVocab.hasLatitude),
		lon = getSingleDouble(point, metaVocab.hasLongitude),
		Option.empty
	)

	protected def getLatLonBox(cov: IRI) = LatLonBox(
		min = Position(
			lat = getSingleDouble(cov, metaVocab.hasSouthernBound),
			lon = getSingleDouble(cov, metaVocab.hasWesternBound),
			Option.empty
		),
		max = Position(
			lat = getSingleDouble(cov, metaVocab.hasNothernBound),
			lon = getSingleDouble(cov, metaVocab.hasEasternBound),
			Option.empty
		),
		label = getOptionalString(cov, RDFS.LABEL),
		uri = Some(cov.toJava)
	)

	protected def getDataProduction(obj: IRI, prod: IRI, fetcher: PlainStaticObjectFetcher) = DataProduction(
		creator = getAgent(getSingleUri(prod, metaVocab.wasPerformedBy)),
		contributors = server.getUriValues(prod, metaVocab.wasParticipatedInBy).map(getAgent),
		host = getOptionalUri(prod, metaVocab.wasHostedBy).map(getOrganization),
		comment = getOptionalString(prod, RDFS.COMMENT),
		sources = server.getUriValues(obj, metaVocab.prov.hadPrimarySource).map(fetcher.getPlainStaticObject).map(_.asUriResource),
		dateTime = getSingleInstant(prod, metaVocab.hasEndTime)
	)

	protected def getSubmission(subm: IRI): DataSubmission = {
		val submitter: IRI = getSingleUri(subm, metaVocab.prov.wasAssociatedWith)
		DataSubmission(
			submitter = getOrganization(submitter),
			start = getSingleInstant(subm, metaVocab.prov.startedAtTime),
			stop = getOptionalInstant(subm, metaVocab.prov.endedAtTime)
		)
	}

	private def getAgent(uri: IRI): Agent = {
		if(getOptionalString(uri, metaVocab.hasFirstName).isDefined)
			getPerson(uri)
		else getOrganization(uri)
	}

	protected def getOrganization(org: IRI) = Organization(
		self = getLabeledResource(org),
		name = getSingleString(org, metaVocab.hasName),
		email = getOptionalString(org, metaVocab.hasEmail)
	)

	protected def getPerson(pers: IRI) = Person(
		self = getLabeledResource(pers),
		firstName = getSingleString(pers, metaVocab.hasFirstName),
		lastName = getSingleString(pers, metaVocab.hasLastName),
		orcid = getOptionalString(pers, metaVocab.hasOrcidId).flatMap(Orcid.unapply)
	)

	private def getProject(project: IRI) = Project(
		self = getLabeledResource(project),
		keywords = getOptionalString(project, metaVocab.hasKeywords).map(s => parseCommaSepList(s).toIndexedSeq)
	)

	private def getDataTheme(theme: IRI) = DataTheme(
		self = getLabeledResource(theme),
		icon = getSingleUriLiteral(theme, metaVocab.hasIcon),
		markerIcon = getOptionalUriLiteral(theme, metaVocab.hasMarkerIcon)
	)

	private def getTemporalCoverage(dobj: IRI) = TemporalCoverage(
		interval = TimeInterval(
			start = getSingleInstant(dobj, metaVocab.hasStartTime),
			stop = getSingleInstant(dobj, metaVocab.hasEndTime)
		),
		resolution = getOptionalString(dobj, metaVocab.hasTemporalResolution)
	)

	private def getStation(stat: IRI) = Station(
		org = getOrganization(stat),
		id = getOptionalString(stat, metaVocab.hasStationId).getOrElse("Unknown"),
		name = getOptionalString(stat, metaVocab.hasName).getOrElse("Unknown"),
		coverage = getStationCoverage(stat),
		responsibleOrganization = getOptionalUri(stat, metaVocab.hasResponsibleOrganization).map(getOrganization),
		sites = Option(server.getUriValues(stat, metaVocab.operatesOn).map(getSite)).filter(_.nonEmpty),
		ecosystems = Option(server.getUriValues(stat, metaVocab.hasEcosystemType).map(getLabeledResource)).filter(_.nonEmpty),
		climateZone = getOptionalUri(stat, metaVocab.hasClimateZone).map(getLabeledResource),
		meanAnnualTemp = getOptionalFloat(stat, metaVocab.hasMeanAnnualTemp),
		operationalPeriod = getOptionalString(stat, metaVocab.hasOperationalPeriod),
		website = getOptionalUriLiteral(stat, RDFS.SEEALSO),
		pictures = Option(server.getUriLiteralValues(stat, metaVocab.hasDepiction)).filter(_.nonEmpty)
	)

	def getOptionalStation(station: IRI): Option[Station] = Try(getStation(station)).toOption

	private def getStationCoverage(stat: IRI): Option[GeoFeature] = {
		val optPoint = for(
			posLat <- getOptionalDouble(stat, metaVocab.hasLatitude);
			posLon <- getOptionalDouble(stat, metaVocab.hasLongitude)
		) yield Position(posLat, posLon, getOptionalFloat(stat, metaVocab.hasElevation))

		optPoint.orElse(getOptionalUri(stat, metaVocab.hasSpatialCoverage).map(getCoverage))
	}

	private def getLocation(location: IRI) = Location(
		geometry = getCoverage(location),
		label = getOptionalString(location, RDFS.LABEL)
	)

	private def getSite(site: IRI) = Site(
		self = getLabeledResource(site),
		ecosystem = getLabeledResource(site, metaVocab.hasEcosystemType),
		location = getOptionalUri(site, metaVocab.hasSpatialCoverage).map(getLocation)
	)

	protected def getL3Meta(dobj: IRI, vtLookup: ValueTypeLookup[IRI], prodOpt: Option[DataProduction]): L3SpecificMeta = {

		val cov = getSingleUri(dobj, metaVocab.hasSpatialCoverage)
		assert(prodOpt.isDefined, "Production info must be provided for a spatial data object")
		val prod = prodOpt.get

		L3SpecificMeta(
			title = getSingleString(dobj, metaVocab.dcterms.title),
			description = getOptionalString(dobj, metaVocab.dcterms.description),
			spatial = getLatLonBox(cov),
			temporal = getTemporalCoverage(dobj),
			productionInfo = prod,
			variables = Some(
				server.getUriValues(dobj, metaVocab.hasActualVariable).flatMap(getL3VarInfo(_, vtLookup))
			).filter(_.nonEmpty)
		)
	}

	protected def getL2Meta(dobj: IRI, vtLookup: ValueTypeLookup[IRI], prod: Option[DataProduction]): L2OrLessSpecificMeta = {
		val acqUri = getSingleUri(dobj, metaVocab.wasAcquiredBy)

		val acq = DataAcquisition(
			station = getStation(getSingleUri(acqUri, metaVocab.prov.wasAssociatedWith)),
			site = getOptionalUri(acqUri, metaVocab.wasPerformedAt).map(getSite),
			interval = for(
				start <- getOptionalInstant(acqUri, metaVocab.prov.startedAtTime);
				stop <- getOptionalInstant(acqUri, metaVocab.prov.endedAtTime)
			) yield TimeInterval(start, stop),
			instrument = server.getUriValues(acqUri, metaVocab.wasPerformedWith).map(_.toJava).toList match{
				case Nil => None
				case single :: Nil => Some(Left(single))
				case many => Some(Right(many))
			},
			samplingPoint = getOptionalUri(acqUri, metaVocab.hasSamplingPoint).map(getPosition),
			samplingHeight = getOptionalFloat(acqUri, metaVocab.hasSamplingHeight)
		)
		val nRows = getOptionalInt(dobj, metaVocab.hasNumberOfRows)

		val coverage = getOptionalUri(dobj, metaVocab.hasSpatialCoverage).map(getCoverage)

		val columns = getOptionalString(dobj, metaVocab.hasActualColumnNames).flatMap(parseJsonStringArray)
			.map{
				_.flatMap{colName =>
					vtLookup.lookup(colName).map{vtUri =>
						val valType = getValueType(vtUri)
						ColumnInfo(colName, valType)
					}
				}.toIndexedSeq
			}.orElse{ //if no actualColumnNames info is available, then all the mandatory columns have to be there
				Some(
					vtLookup.plainMandatory.map{
						case (colName, valTypeIri) => ColumnInfo(colName, getValueType(valTypeIri))
					}
				)
			}.filter(_.nonEmpty)

		L2OrLessSpecificMeta(acq, prod, nRows, coverage, columns)
	}

	private def getCoverage(covUri: IRI): GeoFeature = {
		val covClass = getSingleUri(covUri, RDF.TYPE)

		if(covClass === metaVocab.latLonBoxClass)
			getLatLonBox(covUri)
		else
			GenericGeoFeature(getSingleString(covUri, metaVocab.asGeoJSON))
	}

	protected def getNextVersion(item: IRI): Option[URI] = {
		server.getStatements(None, Some(metaVocab.isNextVersionOf), Some(item))
			.toIndexedSeq.headOption.collect{
				case Rdf4jStatement(next, _, _) => next.toJava
			}
	}

	protected def getPreviousVersion(item: IRI): Option[Either[URI, Seq[URI]]] =
		server.getUriValues(item, metaVocab.isNextVersionOf).map(_.toJava).toList match {
			case Nil => None
			case single :: Nil => Some(Left(single))
			case many => Some(Right(many))
		}

	protected def getPreviousVersions(item: IRI): Seq[URI] = getPreviousVersion(item).fold[Seq[URI]](Nil)(_.fold(Seq(_), identity))

	def getValTypeLookup(datasetSpec: IRI): ValueTypeLookup[IRI] =
		new ValueTypeLookup(getDatasetVars(datasetSpec) ++ getDatasetColumns(datasetSpec))

	private def getL3VarInfo(vi: IRI, vtLookup: ValueTypeLookup[IRI]): Option[L3VarInfo] = for(
		varName <- getOptionalString(vi, RDFS.LABEL);
		valTypeUri <- vtLookup.lookup(varName)
	) yield
		L3VarInfo(
			label = varName,
			valueType = getValueType(valTypeUri),
			minMax = getOptionalDouble(vi, metaVocab.hasMinValue).flatMap{min =>
				getOptionalDouble(vi, metaVocab.hasMaxValue).map(min -> _)
			}
		)


	private def getValueType(vt: IRI) = ValueType(
		getLabeledResource(vt),
		getOptionalUri(vt, metaVocab.hasQuantityKind).map(getLabeledResource),
		getOptionalString(vt, metaVocab.hasUnit)
	)

	private def getDatasetVars(ds: IRI): Seq[DatasetVariable[IRI]] = server.getUriValues(ds, metaVocab.hasVariable).map{dv =>
		new DatasetVariable[IRI](
			title = getSingleString(dv, metaVocab.hasVariableTitle),
			valueType = getSingleUri(dv, metaVocab.hasValueType),
			isRegex = getOptionalBool(dv, metaVocab.isRegexVariable).getOrElse(false),
			isOptional = getOptionalBool(dv, metaVocab.isOptionalVariable).getOrElse(false)
		)
	}

	private def getDatasetColumns(ds: IRI): Seq[DatasetVariable[IRI]] = server.getUriValues(ds, metaVocab.hasColumn).map{dv =>
		new DatasetVariable[IRI](
			title = getSingleString(dv, metaVocab.hasColumnTitle),
			valueType = getSingleUri(dv, metaVocab.hasValueType),
			isRegex = getOptionalBool(dv, metaVocab.isRegexColumn).getOrElse(false),
			isOptional = getOptionalBool(dv, metaVocab.isOptionalColumn).getOrElse(false)
		)
	}
}
