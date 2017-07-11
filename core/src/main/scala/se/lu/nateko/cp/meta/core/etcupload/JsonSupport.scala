package se.lu.nateko.cp.meta.core.etcupload

import se.lu.nateko.cp.meta.core.CommonJsonSupport
import spray.json._

object JsonSupport extends CommonJsonSupport{

	implicit object stationIdFormat extends RootJsonFormat[StationId] {

		def write(id: StationId) = JsString(id.id)

		def read(value: JsValue): StationId = value match{
			case JsString(StationId(id)) => id
			case _ => deserializationError("Expected string of the format CC-Xxx")
		}
	}

	implicit val etcUploatMetaFormat = jsonFormat6(EtcUploadMetadata)
}
