package se.lu.nateko.cp.meta.core.data

import se.lu.nateko.cp.meta.core.CommonJsonSupport
import spray.json._

object JsonSupport extends CommonJsonSupport{

	implicit val uriResourceFormat = jsonFormat2(UriResource)
	implicit val dataObjectSpecFormat = jsonFormat5(DataObjectSpec)

	implicit val dataThemeFormat = enumFormat(DataTheme)
	implicit val orgClassFormat = enumFormat(OrganizationClass)

	implicit val positionFormat = jsonFormat2(Position)
	implicit val spatialCoverageFormat = jsonFormat3(LatLonBox)
	implicit val geoTrackFormat = jsonFormat1(GeoTrack)
	implicit val genericGeoFeatureFormat = jsonFormat1(GenericGeoFeature)

	implicit object geoFeatureFormat extends RootJsonFormat[GeoFeature]{

		def write(geo: GeoFeature): JsValue = geo match {
			case llb: LatLonBox => llb.toJson
			case gt: GeoTrack => gt.toJson
			case pos: Position => pos.toJson
			case ggf: GenericGeoFeature => ggf.toJson
		}

		def read(value: JsValue): GeoFeature = value match {
			case JsObject(fields) =>
				if(fields.contains("points"))
					value.convertTo[GeoTrack]
				else if(fields.contains("min") && fields.contains("max"))
					value.convertTo[LatLonBox]
				else if(fields.contains("lat") && fields.contains("lon"))
					value.convertTo[Position]
				else
					value.convertTo[GenericGeoFeature]
			case _ =>
				deserializationError("Expected a JsObject representing a GeoFeature")
		}
	}

	implicit val orgFormat = jsonFormat3(Organization)
	implicit val personFormat = jsonFormat3(Person)
	implicit val objectProducerFormat = jsonFormat5(Station)

	implicit object agentFormat extends JsonFormat[Agent]{

		def write(agent: Agent): JsValue = agent match{
			case person: Person => person.toJson
			case org: Organization => org.toJson
		}

		def read(value: JsValue): Agent = value match{
			case JsObject(fields) =>
				if(fields.contains("firstName"))
					value.convertTo[Person]
				else
					value.convertTo[Organization]
			case _ =>
				deserializationError("Expected JS object representing eigher a person or an organization")
		}
	}

	implicit val dataProductionFormat = jsonFormat5(DataProduction)
	implicit val dataAcquisitionFormat = jsonFormat2(DataAcquisition)
	implicit val dataSubmissionFormat = jsonFormat3(DataSubmission)

	implicit val temporalCoverageFormat = jsonFormat2(TemporalCoverage)

	implicit val l2SpecificMetaFormat = jsonFormat4(L2OrLessSpecificMeta)
	implicit val l3SpecificMetaFormat = jsonFormat6(L3SpecificMeta)

	implicit val wdcggUploadCompletionFormat = jsonFormat3(WdcggUploadCompletion)
	implicit val ecocsvUploadCompletionFormat = jsonFormat1(TimeSeriesUploadCompletion)
	implicit val socatUploadCompletionFormat = jsonFormat2(SpatialTimeSeriesUploadCompletion)

	implicit object ingestionMetadataExtractFormat extends JsonFormat[IngestionMetadataExtract]{

		def write(uploadInfo: IngestionMetadataExtract): JsValue = uploadInfo match{
			case wdcgg: WdcggUploadCompletion => wdcgg.toJson
			case ts: TimeSeriesUploadCompletion => ts.toJson
			case sts: SpatialTimeSeriesUploadCompletion => sts.toJson
		}

		def read(value: JsValue): IngestionMetadataExtract =  value match {
			case JsObject(fields)  =>
				if(fields.contains("customMetadata"))
					value.convertTo[WdcggUploadCompletion]
				else if(fields.contains("coverage"))
					value.convertTo[SpatialTimeSeriesUploadCompletion]
				else
					value.convertTo[TimeSeriesUploadCompletion]
			case _ =>
				deserializationError("Expected JS object representing upload completion info")
		}
	}

	implicit val uploadCompletionFormat = jsonFormat2(UploadCompletionInfo)
	implicit val dataObjectFormat = jsonFormat10(DataObject)

}
