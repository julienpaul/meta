package se.lu.nateko.cp.meta.persistence.postgres

import java.sql.PreparedStatement
import java.sql.Timestamp
import org.openrdf.model.Literal
import org.openrdf.model.URI
import org.openrdf.model.ValueFactory
import org.openrdf.model.vocabulary.XMLSchema
import se.lu.nateko.cp.meta.instanceserver.RdfUpdate
import se.lu.nateko.cp.meta.persistence.RdfUpdateLog
import se.lu.nateko.cp.meta.RdflogConfig

class PostgresRdfLog(logName: String, serv: DbServer, creds: DbCredentials, factory: ValueFactory) extends RdfUpdateLog{

	if(!isInitialized) initLog()

	def appendAll(updates: TraversableOnce[RdfUpdate]): Unit = {
		//TODO Use a pool of connections/prepared statements for better performance
		val appendPs = getAppendingStatement
		appendPs.clearBatch()

		for(update <- updates){
			appendPs.setTimestamp(1, new Timestamp(System.currentTimeMillis))
			appendPs.setBoolean(2, update.isAssertion)
			appendPs.setString(4, update.statement.getSubject.stringValue)
			appendPs.setString(5, update.statement.getPredicate.stringValue)

			update.statement.getObject match{
				case uri: URI =>
					appendPs.setShort(3, 0) //triple type 0
					appendPs.setString(6, uri.stringValue)
					appendPs.setString(7, null)
				case lit: Literal if lit.getLanguage != null =>
					appendPs.setShort(3, 2) //triple type 2
					appendPs.setString(6, lit.getLabel)
					appendPs.setString(7, lit.getLanguage)
				case lit: Literal =>
					appendPs.setShort(3, 1) //triple type 1
					appendPs.setString(6, lit.getLabel)
					appendPs.setString(7, safeDatatype(lit))
			}

			appendPs.addBatch()
		}
		appendPs.executeBatch()
		appendPs.getConnection.close()
	}

	def updates: Iterator[RdfUpdate] = {
		val conn = getConnection
		val st = conn.createStatement
		val rs = st.executeQuery(s"SELECT * FROM $logName ORDER BY tstamp")
		new RdfUpdateResultSetIterator(rs, factory, () => {st.close; conn.close()})
	}

	def updatesUpTo(time: Timestamp): Iterator[RdfUpdate] = {
		val conn = getConnection
		val ps = conn.prepareStatement(s"SELECT * FROM $logName WHERE tstamp <= ? ORDER BY tstamp")
		ps.setTimestamp(1, time)
		val rs = ps.executeQuery()
		new RdfUpdateResultSetIterator(rs, factory, () => {ps.close(); conn.close()})
	}

	def close(): Unit = {}

	def dropLog(): Unit = execute(s"DROP TABLE IF EXISTS $logName")

	def initLog(): Unit = {

		val createTable =
			s"""CREATE TABLE $logName (
				"tstamp" timestamptz,
				"ASSERTION" boolean,
				"TYPE" smallint,
				"SUBJECT" text,
				"PREDICATE" text,
				"OBJECT" text,
				"LITATTR" text
			) WITH (OIDS=FALSE)"""

		val setOwner = s"ALTER TABLE $logName OWNER TO ${creds.user}"

//		val makeIndexes = Seq("SUBJECT", "PREDICATE", "OBJECT", "LITATTR").map(colName =>{
//			val colLow = colName.toLowerCase
//			s"""CREATE INDEX ${colLow}_index ON rdflog USING btree ("$colName" COLLATE pg_catalog."C")"""
//		})
		val createIndex = s"""CREATE INDEX ${logName}_index ON ${logName} USING btree (tstamp)"""

		val all = createTable +: setOwner +: createIndex +: Nil

		execute(all: _*)
	}

	def isInitialized: Boolean = {
		val meta = getConnection.getMetaData
		val tblRes = meta.getTables(null, null, logName, null)
		val tblPresent = tblRes.next()
		tblRes.close()
		tblPresent
	}

	private def execute(statements: String*): Unit = {
		val conn = getConnection
		val st = conn.createStatement
		statements.foreach(st.execute)
		st.close()
		conn.close()
	}

	private def getConnection = Postgres.getConnection(serv, creds).get

	private def getAppendingStatement: PreparedStatement = {
		val appenderConn = getConnection
		appenderConn.prepareStatement(s"INSERT INTO $logName VALUES (?, ?, ?, ?, ?, ?, ?)")
	}

	private def safeDatatype(lit: Literal): String =
		if(lit.getDatatype == null) XMLSchema.STRING.stringValue
		else lit.getDatatype.stringValue

}

object PostgresRdfLog{

	def apply(name: String, conf: RdflogConfig, factory: ValueFactory) =
		new PostgresRdfLog(
			logName = name,
			serv = conf.server,
			creds = conf.credentials,
			factory = factory
		)

}
