package se.lu.nateko.cp.meta.utils.sesame

import org.openrdf.repository.Repository
import org.openrdf.repository.sail.SailRepository
import org.openrdf.sail.memory.MemoryStore
import org.openrdf.rio.RDFFormat
import org.openrdf.model.Statement
import org.openrdf.model.URI
import scala.util.Failure
import scala.util.Try

object Loading {

	def fromResource(path: String, baseUri: String): Repository = fromResource(path, baseUri, RDFFormat.RDFXML)

	def fromResource(path: String, baseUri: String, format: RDFFormat): Repository = {
		val repo = empty
		loadResource(repo, path, baseUri, format).get //will cast an exception if loading failed
		repo
	}

	def loadResource(repo: Repository, path: String, baseUri: String, format: RDFFormat): Try[Unit] = {
		val instStream = getClass.getResourceAsStream(path)
		val ontUri = repo.getValueFactory.createURI(baseUri)
		repo.transact(conn => {
			conn.add(instStream, baseUri, format, ontUri)
		})
	}

	def empty: Repository = {
		val repo = new SailRepository(new MemoryStore)
		repo.initialize()
		repo
	}

	private val chunkSize = 1000

	def fromStatements(statements: Iterator[Statement], contexts: URI*): Repository = {

		val repo = Loading.empty

		def commitChunk(chunk: Seq[Statement]): Try[Unit] =
			repo.transact(conn => {
				for(statement <- chunk){
					conn.add(statement, contexts :_*)
				}
			})

		statements.sliding(chunkSize, chunkSize)
			.map(commitChunk)
			.collectFirst{case Failure(err) => err}
			match {
			case None => repo
			case Some(err) => throw err
		}
	}
}