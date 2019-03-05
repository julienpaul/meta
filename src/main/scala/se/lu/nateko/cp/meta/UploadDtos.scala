package se.lu.nateko.cp.meta

import java.net.URI
import java.time.Instant

import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import se.lu.nateko.cp.meta.core.data._

sealed trait UploadDto{
	def submitterId: String
	def isNextVersionOf: Option[Sha256Sum]
	def preExistingDoi: Option[String]
}

sealed trait ObjectUploadDto extends UploadDto {
	def hashSum: Sha256Sum
	def fileName: String
}

case class DataObjectDto(
	hashSum: Sha256Sum,
	submitterId: String,
	objectSpecification: URI,
	fileName: String,
	specificInfo: Either[ElaboratedProductMetadata, StationDataMetadata],
	isNextVersionOf: Option[Sha256Sum],
	preExistingDoi: Option[String]
) extends ObjectUploadDto

case class DocObjectDto(
	hashSum: Sha256Sum,
	submitterId: String,
	fileName: String,
	isNextVersionOf: Option[Sha256Sum],
	preExistingDoi: Option[String]
) extends ObjectUploadDto


case class StaticCollectionDto(
	submitterId: String,
	members: Seq[URI],
	title: String,
	description: Option[String],
	isNextVersionOf: Option[Sha256Sum],
	preExistingDoi: Option[String]
) extends UploadDto

case class StationDataMetadata(
	station: URI,
	instrument: Option[Either[URI, Seq[URI]]],
	samplingHeight: Option[Float],
	acquisitionInterval: Option[TimeInterval],
	nRows: Option[Int],
	production: Option[DataProductionDto]
){
	def instruments: Seq[URI] = instrument.fold(Seq.empty[URI])(_.fold(Seq(_), identity))
}

case class ElaboratedProductMetadata(
	title: String,
	description: Option[String],
	spatial: Either[LatLonBox, URI],
	temporal: TemporalCoverage,
	production: DataProductionDto,
	customLandingPage: Option[URI]
)

case class DataProductionDto(
	creator: URI,
	contributors: Seq[URI],
	hostOrganization: Option[URI],
	comment: Option[String],
	creationDate: Instant
)

case class SubmitterProfile(id: String, producingOrganizationClass: Option[URI])
