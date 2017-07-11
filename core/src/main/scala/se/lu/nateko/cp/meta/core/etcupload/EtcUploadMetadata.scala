package se.lu.nateko.cp.meta.core.etcupload

import java.time.LocalDateTime
import se.lu.nateko.cp.meta.core.crypto.Sha256Sum

case class EtcUploadMetadata(
	hashSum: Sha256Sum,
	fileName: String,
	station: StationId,
	logger: Int,
	acquisitionStart: LocalDateTime,
	acquisitionStop: LocalDateTime
)
