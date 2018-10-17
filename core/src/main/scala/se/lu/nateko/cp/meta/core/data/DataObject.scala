package se.lu.nateko.cp.meta.core.data

import java.net.URI
import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import java.time.Instant

import spray.json.JsValue

case class UriResource(uri: URI, label: Option[String])

sealed trait Agent{
	val self: UriResource
}
case class Organization(self: UriResource, name: String) extends Agent
case class Person(self: UriResource, firstName: String, lastName: String) extends Agent


case class Station(
	org: Organization,
	id: String,
	name: String,
	coverage: Option[GeoFeature]
)

case class DataTheme(self: UriResource, icon: URI, markerIcon: Option[URI])

case class DataObjectSpec(
	self: UriResource,
	project: UriResource,
	theme: DataTheme,
	format: UriResource,
	encoding: UriResource,
	dataLevel: Int,
	datasetSpec: Option[JsValue]
)

case class DataAcquisition(
	station: Station,
	interval: Option[TimeInterval],
	instrument: Option[URI],
	samplingHeight: Option[Float]
)

case class DataProduction(
	creator: Agent,
	contributors: Seq[Agent],
	host: Option[Organization],
	comment: Option[String],
	dateTime: Instant
)
case class DataSubmission(submitter: Organization, start: Instant, stop: Option[Instant])

case class L2OrLessSpecificMeta(
	acquisition: DataAcquisition,
	productionInfo: Option[DataProduction],
	nRows: Option[Int],
	coverage: Option[GeoFeature]
)

case class L3SpecificMeta(
	title: String,
	description: Option[String],
	spatial: LatLonBox,
	temporal: TemporalCoverage,
	productionInfo: DataProduction
)

sealed trait DataAffiliation
case object Icos extends DataAffiliation
case class OrgAffiliation(org: Organization) extends DataAffiliation

case class DataObject(
	hash: Sha256Sum,
	accessUrl: Option[URI],
	pid: Option[String],
	doi: Option[String],
	fileName: String,
	size: Option[Long],
	submission: DataSubmission,
	specification: DataObjectSpec,
	specificInfo: Either[L3SpecificMeta, L2OrLessSpecificMeta],
	previousVersion: Option[URI],
	nextVersion: Option[URI],
	parentCollections: Seq[UriResource]
){
	def production: Option[DataProduction] = specificInfo.fold(
		l3 => Some(l3.productionInfo),
		l2 => l2.productionInfo
	)

	def coverage: Option[GeoFeature] = specificInfo.fold(
		l3 => Some(l3.spatial),
		l2 => l2.coverage.orElse(l2.acquisition.station.coverage)
	)
}

sealed trait DataItem

sealed trait StaticDataItem extends DataItem

final case class PlainDataObject(res: URI, hash: Sha256Sum, name: String) extends StaticDataItem

sealed trait DataItemCollection extends DataItem {
	type M <: DataItem
	def members: Seq[M]
	def creator: Organization
	def title: String
	def description: Option[String]
	def doi: Option[String]
}

final case class StaticCollection(
	res: URI,
	members: Seq[StaticDataItem],
	creator: Organization,
	title: String,
	description: Option[String],
	previousVersion: Option[URI],
	nextVersion: Option[URI],
	doi: Option[String]
) extends DataItemCollection with StaticDataItem {
	type M = StaticDataItem
}
