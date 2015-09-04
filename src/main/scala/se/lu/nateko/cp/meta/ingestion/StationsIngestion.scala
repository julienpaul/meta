package se.lu.nateko.cp.meta.ingestion

import java.net.{URLEncoder, URI}

import org.openrdf.model.vocabulary.{XMLSchema, RDF}
import org.openrdf.model.{ValueFactory, Statement}


case class Station(
	val classUri: URI,
	val shortName: String,
	val longName: String,
	val stationClass: Int,
	val country: String,
	val location: Either[String, (Double, Double)],
	val elevationAG: Option[String],
	val elevationAS: Option[Float],
	val fundingForConstruction: String,
	val fundingForOperation: String,
	val siteType: String,
	val operationalDateEstimate: String,
	val stationKind: String,
	val isOperational: Boolean,
	val hasPreIcosMeas: Boolean,
	val piName: String,
	val piEmail: String
)

object StationsIngestion extends Ingester{

	val prefix = "http://meta.icos-cp.eu/ontologies/stationentry/"

	def uri(fragment: String)(implicit valueFactory: ValueFactory) = valueFactory.createURI(prefix + fragment)
	def lit(lit: String, dtype: org.openrdf.model.URI)(implicit factory: ValueFactory) = factory.createLiteral(lit, dtype)
	def lit(lit: String)(implicit factory: ValueFactory) = factory.createLiteral(lit, XMLSchema.STRING)
	def lit(lit: Int)(implicit factory: ValueFactory) = factory.createLiteral(lit)
	def lit(lit: Boolean)(implicit factory: ValueFactory) = factory.createLiteral(lit)
	def lit(lit: Double)(implicit factory: ValueFactory) = factory.createLiteral(lit)

	val es = new URI(prefix + "ES")
	val as = new URI(prefix + "AS")
	val os = new URI(prefix + "OS")

	val themeToUri = Map(
		"eco" -> es,
		"atm" -> as,
		"ocean" -> os,
		"oce" -> os
	)


	def getStationsTable: TextTable = {
		val stationsStream = getClass.getResourceAsStream("/stations.csv")
		new TsvDataTable(stationsStream)
	}

	def rowToStation(row: TextTableRow): Station = {

		def opt(ind: Int): Option[String] = {
			val cell = row(ind)
			if(cell == null || cell.isEmpty) None
			else Some(cell.trim)
		}

		def location: Either[String, (Double, Double)] =
			(dblOpt(4), dblOpt(5)) match{
				case (Some(lat), Some(lon)) =>
					Right((lat, lon))
				case _ =>
					val locDescr: String = Seq(opt(4), opt(5)).flatten.distinct.mkString(", ")
					Left(if(locDescr.isEmpty) "Undefined" else locDescr)
			}


		def dbl(ind: Int): Double = {
			row(ind).trim.replace(',', '.').toDouble
		}

		def dblOpt(ind: Int): Option[Double] =
			try{Some(dbl(ind))}
			catch{ case _ : Throwable => None}

		Station(
			shortName = row(0),
			longName = row(1),
			classUri = themeToUri(row(2).toLowerCase.trim),
			country = row(3),
			location = location,
			elevationAS = opt(6).map(_.toFloat),
			elevationAG = opt(7),
			stationClass = row(8).toInt,
			siteType = row(9),
			stationKind = row(10),
			piName = row(11),
			piEmail = row(12),
			hasPreIcosMeas = row(13).toUpperCase.trim == "YES",
			operationalDateEstimate = row(14),
			isOperational = row(15).toUpperCase.trim == "X",
			fundingForConstruction = row(16),
			fundingForOperation = row(17)
		)
	}


	def stationToStatements(station: Station, factory: ValueFactory): Seq[Statement] = {
		implicit val f = factory
		def netToSesame(uri: URI) = factory.createURI(uri.toString)

		val statUri = factory.createURI(station.classUri.toString + "/" + URLEncoder.encode(station.shortName, "UTF-8"))

		val conditionals = Seq(
			station.elevationAS.map{elevation =>
				(uri("hasElevationAboveSea"), lit(elevation.toString, XMLSchema.FLOAT))
			},
			station.elevationAG.map{elevation =>
				(uri("hasElevationAboveGround"), lit(elevation))
			}
		).flatten

		val position = station.location match {
			case Left(pos) => Seq(
				(uri("hasLocationDescription"), lit(pos))
			)
			case Right((lat, lon)) => Seq(
				(uri("hasLat"), lit(lat)),
				(uri("hasLon"), lit(lon))
			)
		}

		(position ++ conditionals ++ Seq(
			(RDF.TYPE, netToSesame(station.classUri)),
			(uri("hasShortName"), lit(station.shortName)),
			(uri("hasLongName"), lit(station.longName)),
			(uri("hasCountry"), lit(station.country)),
			(uri("hasStationClass"), lit(station.stationClass)),
			(uri("hasSiteType"), lit(station.siteType)),
			(uri("hasStationKind"), lit(station.stationKind)),
			(uri("hasPiName"), lit(station.piName)),
			(uri("hasPiEmail"), lit(station.piEmail)),
			(uri("hasPreIcosMeasurements"), lit(station.hasPreIcosMeas)),
			(uri("hasOperationalDateEstimate"), lit(station.operationalDateEstimate)),
			(uri("isAlreadyOperational"), lit(station.isOperational)),
			(uri("hasFundingForConstruction"), lit(station.fundingForConstruction)),
			(uri("hasFundingForOperation"), lit(station.fundingForOperation))
		)).map{
			case (pred, obj) => factory.createStatement(statUri, pred, obj)
		}
	}

	override def getStatements(valueFactory: ValueFactory): Iterator[Statement] =
		getStationsTable.rows.iterator
			.flatMap(row => {
				val station = rowToStation(row)
				stationToStatements(station, valueFactory)
			})
}
