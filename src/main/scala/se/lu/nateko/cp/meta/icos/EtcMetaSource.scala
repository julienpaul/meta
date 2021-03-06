package se.lu.nateko.cp.meta.icos

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Failure

import akka.NotUsed
import akka.actor.ActorSystem
import akka.actor.Cancellable
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri
import akka.stream.ActorAttributes
import akka.stream.Materializer
import akka.stream.Supervision
import akka.stream.scaladsl.Source
import akka.util.ByteString
import se.lu.nateko.cp.meta.EtcUploadConfig
import se.lu.nateko.cp.meta.api.UriId
import se.lu.nateko.cp.meta.core.data.Position
import se.lu.nateko.cp.meta.core.etcupload.StationId
import se.lu.nateko.cp.meta.ingestion.badm.Badm
import se.lu.nateko.cp.meta.ingestion.badm.BadmEntry
import se.lu.nateko.cp.meta.ingestion.badm.BadmLocalDate
import se.lu.nateko.cp.meta.ingestion.badm.BadmValue
import se.lu.nateko.cp.meta.ingestion.badm.EtcEntriesFetcher
import se.lu.nateko.cp.meta.ingestion.badm.Parser
import se.lu.nateko.cp.meta.services.CpVocab
import se.lu.nateko.cp.meta.utils.Validated
import se.lu.nateko.cp.meta.utils.urlEncode
import se.lu.nateko.cp.meta.ingestion.badm.BadmLocalDateTime
import se.lu.nateko.cp.meta.core.data.Orcid

class EtcMetaSource(implicit system: ActorSystem, mat: Materializer) extends TcMetaSource[ETC.type] {
	import EtcMetaSource._
	import system.dispatcher

	def state: Source[State, Cancellable] = Source
		.tick(35.seconds, 5.hours, NotUsed)
		.mapAsync(1){_ =>
			fetchFromEtc().andThen{
				case Failure(err) =>
					system.log.error(err, "ETC metadata fetching/parsing error")
			}
		}
		.withAttributes(ActorAttributes.supervisionStrategy(_ => Supervision.Resume))
		.mapConcat{validated =>
			if(!validated.errors.isEmpty) system.log.warning("ETC metadata problem(s): " + validated.errors.mkString("\n"))
			validated.result.toList
		}


	def fetchFromEtc(): Future[Validated[State]] = {
		val peopleFut = fetchFromTsv("team", getPerson(_))
		val stationsFut = fetchFromTsv("station", getStation(_))

		val futfutValVal = for(
			peopleVal <- peopleFut;
			stationsVal <- stationsFut
		) yield Validated.liftFuture{
			for(
				people <- peopleVal;
				stations <- stationsVal
			) yield {
				val membExtractor: Lookup => Validated[EtcMembership] = getMembership(
					people.flatMap(p => p.tcIdOpt.map(_ -> p)).toMap,
					stations.map(s => s.tcId -> s).toMap
				)(_)
				fetchFromTsv("teamrole", membExtractor).map(_.map{membs =>
					//TODO Add instruments info
					//TODO Consider that after mapping to CP roles, a person may (in theory) have duplicate roles at the same station
					new TcState(stations, membs, Nil)
				})
			}
		}

		futfutValVal.flatten.map(_.flatMap(identity))
	}

	private def fetchFromTsv[T](tableType: String, extractor: Lookup => Validated[T]): Future[Validated[Seq[T]]] = Http()
		.singleRequest(HttpRequest(
			uri = baseEtcApiUrl.withQuery(Uri.Query("type" -> tableType))
		))
		.flatMap(_.entity.toStrict(3.seconds))
		.map(ent => Validated.sequence(parseTsv(ent.data).map(extractor)))

}

object EtcMetaSource{

	val baseEtcApiUrl = Uri("http://gaia.agraria.unitus.it:89/cpmeta")
	type Lookup = Map[String, String]
	type E = ETC.type
	//type EtcInstrument = Instrument[E]
	type EtcPerson = Person[E]
	type EtcStation = TcStationaryStation[E]
	type EtcMembership = Membership[E]

	def makeId(id: String): TcId[E] = TcConf.EtcConf.makeId(id)

	object Vars{
		val lat = "LOCATION_LAT"
		val lon = "LOCATION_LONG"
		val elev = "LOCATION_ELEV"
		val fname = "TEAM_MEMBER_FIRSTNAME"
		val lname = "TEAM_MEMBER_LASTNAME"
		val email = "TEAM_MEMBER_EMAIL"
		val orcid = "TEAM_MEMBER_ORCID"
		val role = "TEAM_MEMBER_ROLE"
		val roleEnd = "TEAM_MEMBER_WORKEND"
		val persId = "ID_TEAM"
		val stationTcId = "ID_STATION"
		val siteName = "SITE_NAME"
		val siteId = "SITE_ID"
	}

	val rolesLookup: Map[String, Option[Role]] = Map(
		"PI"         -> Some(PI),
		"CO-PI"      -> Some(Researcher),
		"MANAGER"    -> Some(Administrator),
		"SCI"        -> Some(Researcher),
		"SCI-FLX"    -> Some(Researcher),
		"SCI-ANC"    -> Some(Researcher),
		"TEC"        -> Some(Engineer),
		"TEC-FLX"    -> Some(Engineer),
		"TEC-ANC"    -> Some(Engineer),
		"DATA"       -> Some(DataManager),
		"ADMIN"      -> None,
		"AFFILIATED" -> None
	)

	// def getInstruments(stId: StationId, badms: Seq[BadmEntry])(implicit tcConf: TcConf[ETC.type]): Seq[Validated[EtcInstrument]] = {
	// 	val sid = stId.id
	// 	badms.filter(_.variable == "GRP_LOGGER").map{badm =>
	// 		implicit val lookup = toLookup(Seq(badm))
	// 		for(
	// 			lid <- getNumber("GRP_LOGGER/LOGGER_ID").require(s"a logger at $sid has no id");
	// 			model <- getString("GRP_LOGGER/LOGGER_MODEL").require(s"a logger $lid at $sid has no model");
	// 			sn <- lookUp("GRP_LOGGER/LOGGER_SN").require(s"a logger $lid at $sid has no serial number");
	// 			cpId = CpVocab.getEtcInstrId(stId, lid.intValue)
	// 		) yield
	// 			//TODO Use TC-stable station id as component of tcId
	// 			Instrument(cpId, makeId(s"${sid}_$lid"), model, sn.valueStr)
	// 	}
	// }

	def lookUp(varName: String)(implicit lookup: Lookup): Validated[String] =
		new Validated(lookup.get(varName).filter(_.length > 0))

	def lookUpOrcid(varName: String)(implicit lookup: Lookup): Validated[Option[Orcid]] =
		lookUp(varName).optional.flatMap{
			case Some(Orcid(orc)) => Validated.ok(Some(orc))
			case None => Validated.ok(None)
			case Some(badOrcid) => new Validated(None, Seq(s"Could not parse Orcid id from string $badOrcid"))
		}

	def getNumber(varName: String)(implicit lookup: Lookup): Validated[Number] = lookUp(varName).flatMap{
		str => Validated(Badm.numParser.parse(str)).require(s"$varName must have been a number (was $str)")
	}

	def getLocalDate(varName: String)(implicit lookup: Lookup): Validated[Instant] = lookUp(varName).flatMap{
		case Badm.Date(BadmLocalDateTime(dt)) => Validated.ok(toCET(dt))
		case Badm.Date(BadmLocalDate(date)) => Validated.ok(toCETnoon(date))
		case bv => Validated.error(s"$varName must have been a local date(Time) (was $bv)")
	}

	def getPosition(implicit lookup: Lookup): Validated[Position] =
		for(
			lat <- getNumber(Vars.lat).require("station must have latitude");
			lon <- getNumber(Vars.lon).require("station must have longitude");
			alt <- getNumber(Vars.elev).optional
		) yield Position(lat.doubleValue, lon.doubleValue, alt.map(_.floatValue))

	def getPerson(implicit lookup: Lookup): Validated[EtcPerson] =
		for(
			fname <- lookUp(Vars.fname).require("person must have first name");
			lname <- lookUp(Vars.lname).require("person must have last name");
			tcId <- lookUp(Vars.persId).require("unique ETC's id is required for a person");
			email <- lookUp(Vars.email).optional;
			orcid <- lookUpOrcid(Vars.orcid);
			cpId = CpVocab.getPersonCpId(fname, lname)
		) yield
			Person(cpId, Some(makeId(tcId)), fname, lname, email.map(_.toLowerCase), orcid)

	def getCountryCode(stId: StationId): Validated[CountryCode] = getCountryCode(stId.id.take(2))

	def getCountryCode(s: String): Validated[CountryCode] = s match{
		case CountryCode(cc) => Validated.ok(cc)
		case _ => Validated.error(s + " is not a valid country code")
	}

	def toCET(ldt: LocalDateTime): Instant = ldt.atOffset(ZoneOffset.ofHours(1)).toInstant
	def toCETnoon(ld: LocalDate): Instant = toCET(LocalDateTime.of(ld, LocalTime.of(12, 0)))

	def parseTsv(bs: ByteString): Seq[Lookup] = {
		val lines = scala.io.Source.fromString(bs.utf8String).getLines()
		val colNames: Array[String] = lines.next().split('\t').map(_.trim)
		lines.map{lineStr =>
			colNames.zip(lineStr.split('\t').map(_.trim)).toMap
		}.toSeq
	}

	def getStation(implicit lookup: Lookup): Validated[EtcStation] = for(
		pos <- getPosition;
		tcId <- lookUp(Vars.stationTcId);
		name <- lookUp(Vars.siteName);
		id <- lookUp(Vars.siteId);
		countryCode <- getCountryCode(id.take(2))
	) yield
		TcStationaryStation(TcConf.stationId[E](id), makeId(tcId), name, id, Some(countryCode), pos)

	def getMembership(
		people: Map[TcId[E], EtcPerson],
		stations: Map[TcId[E], EtcStation]
	)(implicit lookup: Lookup): Validated[EtcMembership] = for(
		persId <- lookUp(Vars.persId).require(s"${Vars.persId} is required for membership info");
		stationTcId <- lookUp(Vars.stationTcId).require(s"${Vars.stationTcId} is required for membership info");
		roleStr <- lookUp(Vars.role).require(s"${Vars.role} is required for membership info");
		roleOpt <- new Validated(rolesLookup.get(roleStr)).require(s"Unknown ETC role: $roleStr");
		role <- new Validated(roleOpt);
		roleEnd <- getLocalDate(Vars.roleEnd).optional;
		person <- new Validated(people.get(makeId(persId))).require(s"Person not found for tcId = $persId");
		station <- new Validated(stations.get(makeId(stationTcId))).require(s"Station not found for tcId = $persId")
	) yield {
		val assumedRole = new AssumedRole[E](role, person, station, None)
		Membership(UriId(""), assumedRole, None, roleEnd)
	}

}
