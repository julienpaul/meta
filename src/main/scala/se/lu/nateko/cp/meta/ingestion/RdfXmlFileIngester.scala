package se.lu.nateko.cp.meta.ingestion

import org.openrdf.rio.RDFFormat
import se.lu.nateko.cp.meta.utils.sesame._
import org.openrdf.model.ValueFactory
import org.openrdf.model.Statement


//TODO Consider rewriting using a parser only, without loading all statements into memory
class RdfFileIngester(resourcePath: String, format: RDFFormat) extends Ingester{

	private[this] val baseUri = "http://dummy.org"

	def getStatements(valueFactory: ValueFactory): Iterator[Statement] = {

		val repo = Loading.fromResource(resourcePath, baseUri, format)

		repo.access(
			_.getStatements(null, null, null, false, valueFactory.createURI(baseUri)),
			repo.shutDown
		)

	}
}

class RdfXmlFileIngester(resourcePath: String) extends RdfFileIngester(resourcePath, RDFFormat.RDFXML)
