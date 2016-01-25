package se.lu.nateko.cp.meta.core.data

import java.net.URI
import se.lu.nateko.cp.meta.core.crypto.Sha256Sum
import java.time.Instant

case class UriResource(uri: URI, label: Option[String])

case class DataObjectSpec(format: URI, encoding: URI, dataLevel: Int)

case class DataSubmission(submitter: UriResource, start: Instant, stop: Option[Instant])
case class DataProduction(producer: UriResource, start: Instant, stop: Instant)

case class DataObject(
	hash: Sha256Sum,
	accessUrl: URI,
	fileName: Option[String],
	production: DataProduction,
	submission: DataSubmission,
	specification: DataObjectSpec
)

