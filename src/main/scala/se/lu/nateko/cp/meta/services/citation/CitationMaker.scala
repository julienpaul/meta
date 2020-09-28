package se.lu.nateko.cp.meta.services.citation

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId

import se.lu.nateko.cp.meta.core.data.DataObject
import se.lu.nateko.cp.meta.core.data.StaticObject
import se.lu.nateko.cp.meta.utils.rdf4j._
import se.lu.nateko.cp.meta.services.CpVocab
import se.lu.nateko.cp.meta.core.MetaCoreConfig

import org.eclipse.rdf4j.repository.Repository
import se.lu.nateko.cp.meta.core.data.TimeInterval


class CitationMaker(doiCiter: PlainDoiCiter, repo: Repository, coreConf: MetaCoreConfig) {
	private implicit val envriConfs = coreConf.envriConfigs

	private val attrProvider = new AttributionProvider(repo)
	val vocab = new CpVocab(repo.getValueFactory)

	def getDataCiteCitation(dobj: StaticObject): Option[String] = for(
		doiStr <- dobj.doi;
		doi <- Doi.unapply(doiStr);
		cit <- doiCiter.getCitationEager(doi)
	) yield cit

	def getIcosCitation(dobj: DataObject): Option[String] = {
		val isIcos: Option[Unit] = if(dobj.specification.project.self.uri === vocab.icosProject) Some(()) else None
		val zoneId = ZoneId.of("UTC")

		def titleOpt = dobj.specificInfo.fold(
			l3 => Some(l3.title),
			l2 => for(
					spec <- dobj.specification.self.label;
					acq = l2.acquisition;
					interval <- acq.interval
				) yield {
					val station = acq.station.name
					val height = acq.samplingHeight.fold("")(sh => s" ($sh m)")
					val time = getTimeFromInterval(interval, zoneId)
					s"$spec, $station$height, $time"
				}
		)
		for(
			_ <- isIcos;
			title <- titleOpt;
			pid <- dobj.doi.orElse(dobj.pid);
			productionInstant <- AttributionProvider.productionTime(dobj)
		) yield {
			val authors = attrProvider.getAuthors(dobj).map{p => s"${p.lastName}, ${p.firstName.head}., "}.mkString
			val handleProxy = if(dobj.doi.isDefined) coreConf.handleProxies.doi else coreConf.handleProxies.basic
			val year = formatDate(productionInstant, zoneId).take(4)
			s"${authors}ICOS RI, $year. $title, ${handleProxy}$pid"
		}
	}

	private def getTimeFromInterval(interval: TimeInterval, zoneId: ZoneId): String = {
		val duration = Duration.between(interval.start, interval.stop)
		if (duration.getSeconds < 24 * 3601) { //daily data object
			val middle = Instant.ofEpochMilli((interval.start.toEpochMilli + interval.stop.toEpochMilli) / 2)
			formatDate(middle, zoneId)
		} else {
			val from = formatDate(interval.start, zoneId)
			val to = formatDate(interval.stop, zoneId)
			s"$from–$to"
		}
	}

	private def formatDate(inst: Instant, zoneId: ZoneId): String = DateTimeFormatter.ISO_LOCAL_DATE.withZone(zoneId).format(inst)

}