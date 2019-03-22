package se.lu.nateko.cp.meta.test.instanceserver

import com.ontotext.graphdb.example.util.EmbeddedGraphDB
import se.lu.nateko.cp.meta.utils.rdf4j._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem

object GraphDbTest extends App {

//	val exeServ = java.util.concurrent.Executors.newSingleThreadExecutor
//	implicit val ctxt = ExecutionContext.fromExecutorService(exeServ)

	val system = ActorSystem("graph_db_test")
	import system.dispatcher

	val db = new EmbeddedGraphDB("/home/oleg/workspace/meta/rdfStorage")
	val repo = db.getRepository("icoscp")

	val doneFut = Future{

		def getSize = repo.accessEagerly{conn =>
			conn.hasStatement(null, null, null, false)
			conn.size()
		}

		getSize
		getSize
		val size = getSize
		println("Size = " + size)


	}

	Await.ready(doneFut, Duration.Inf)
//	ctxt.shutdown()
//	exeServ.shutdown()

	repo.shutDown()
	db.close()
	system.terminate()
	println("all done ok!")
}
