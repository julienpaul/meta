package se.lu.nateko.cp.meta

import spray.json._
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.meta.core.CommonJsonSupport
import se.lu.nateko.cp.meta.core.data.JsonSupport._


object CpmetaJsonProtocol{
	implicit class ExtendableJsObj(val js: JsValue) extends AnyVal{
		def +(kv: (String, String)): JsObject = {
			val jsKv: (String, JsValue) = (kv._1, JsString(kv._2))
			JsObject(js.asJsObject.fields + jsKv)
		}
	}
}

trait CpmetaJsonProtocol extends CommonJsonSupport{
	import CpmetaJsonProtocol._
	import se.lu.nateko.cp.meta.core.crypto.Sha256Sum._

	implicit val resourceDtoFormat = jsonFormat3(ResourceDto)
	implicit val literalValueDtoFormat = jsonFormat2(LiteralValueDto)
	implicit val objectValueDtoFormat = jsonFormat2(ObjectValueDto)

	implicit val minRestrictionDtoFormat = jsonFormat1(MinRestrictionDto)
	implicit val maxRestrictionDtoFormat = jsonFormat1(MaxRestrictionDto)
	implicit val regexpRestrictionDtoFormat = jsonFormat1(RegexpRestrictionDto)
	implicit val oneOfRestrictionDtoFormat = jsonFormat1(OneOfRestrictionDto)

	implicit object ValueDtoFormat extends JsonFormat[ValueDto]{
		override def write(dto: ValueDto) = dto match{
			case dto: LiteralValueDto => dto.toJson + ("type" -> "literal")
			case dto: ObjectValueDto => dto.toJson + ("type" -> "object")
		}
		override def read(value: JsValue) = ???
	}

	implicit object RestrictionDtoFormat extends JsonFormat[DataRestrictionDto]{
		override def write(dto: DataRestrictionDto) = dto match{
			case dto: MinRestrictionDto => dto.toJson + ("type" -> "minValue")
			case dto: MaxRestrictionDto => dto.toJson + ("type" -> "maxValue")
			case dto: RegexpRestrictionDto => dto.toJson + ("type" -> "regExp")
			case dto: OneOfRestrictionDto => dto.toJson + ("type" -> "oneOf")
		}
		override def read(value: JsValue) = ???
	}

	implicit val dataRangeDtoFormat = jsonFormat2(DataRangeDto)
	implicit val cardinalityDtoFormat = jsonFormat2(CardinalityDto)
	implicit val dataPropertyDtoFormat = jsonFormat3(DataPropertyDto)
	implicit val objectPropertyDtoFormat = jsonFormat3(ObjectPropertyDto)

	implicit object PropertyDtoFormat extends JsonFormat[PropertyDto]{
		override def write(dto: PropertyDto) = dto match{
			case dto: DataPropertyDto => dto.toJson + ("type" -> "dataProperty")
			case dto: ObjectPropertyDto => dto.toJson + ("type" -> "objectProperty")
		}
		override def read(value: JsValue) = ???
	}
	
	implicit val classDtoFormat = jsonFormat2(ClassDto)
	implicit val classInfoDtoFormat = jsonFormat3(ClassInfoDto)
	implicit val individualDtoFormat = jsonFormat3(IndividualDto)
	implicit val updateDtoFormat = jsonFormat4(UpdateDto)
	implicit val replaceDtoFormat = jsonFormat4(ReplaceDto)

	implicit val dataProductionDtoFormat = jsonFormat5(DataProductionDto)
	implicit val stationDataMetadataFormat = jsonFormat6(StationDataMetadata)
	implicit val elaboratedProductMetadataFormat = jsonFormat6(ElaboratedProductMetadata)
	implicit val uploadMetadataDtoFormat = jsonFormat6(UploadMetadataDto)

	implicit val userIdFormat = jsonFormat1(UserId)

	implicit val fileDeletionFormat = jsonFormat2(FileDeletionDto)
	implicit val labelingUserFormat = jsonFormat9(LabelingUserDto)
	implicit val labelingStatusUpdateFormat = jsonFormat2(LabelingStatusUpdate)
}
