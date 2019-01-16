package se.lu.nateko.cp.meta.icos

import org.eclipse.rdf4j.model.IRI

import se.lu.nateko.cp.meta.core.etcupload.{ StationId => EtcStationId }
import se.lu.nateko.cp.meta.services.CpVocab
import se.lu.nateko.cp.meta.services.CpmetaVocab


sealed trait TcId[+T <: TC]{
	def id: String
}

trait TcConf[+T <: TC]{
	private case class Id(val id: String) extends TcId[T]
	def makeId(id: String): TcId[T] = new Id(id)
	def tc: T
	def stationClass(meta: CpmetaVocab): IRI
	def tcIdPredicate(meta: CpmetaVocab): IRI
	def makeStation(vocab: CpVocab, stationId: String): IRI
}

object TcConf{

	implicit object AtcConf extends TcConf[ATC.type]{

		val tc = ATC

		def stationClass(meta: CpmetaVocab) = meta.atmoStationClass

		def tcIdPredicate(meta: CpmetaVocab) = meta.hasAtcId

		def makeStation(vocab: CpVocab, stationId: String) = vocab.getAtmosphericStation(stationId)
	}

	implicit object EtcConf extends TcConf[ETC.type]{

		val tc = ETC

		def stationClass(meta: CpmetaVocab) = meta.ecoStationClass

		def tcIdPredicate(meta: CpmetaVocab) = meta.hasEtcId

		def makeStation(vocab: CpVocab, stationId: String) = {
			val EtcStationId(id) = stationId
			vocab.getEcosystemStation(id)
		}
	}

	implicit object OtcConf extends TcConf[OTC.type]{

		val tc = OTC

		def stationClass(meta: CpmetaVocab) = meta.oceStationClass

		def tcIdPredicate(meta: CpmetaVocab) = meta.hasOtcId

		def makeStation(vocab: CpVocab, stationId: String) = vocab.getOceanStation(stationId)
	}

	def makeId[T <: TC](id: String)(implicit conf: TcConf[T]): TcId[T] = conf.makeId(id)
}
