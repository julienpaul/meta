package se.lu.nateko.cp.meta.persistence

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import java.sql.Timestamp

class InMemoryRdfLog extends RdfUpdateLog{

	private[this] val log = new ConcurrentLinkedQueue[RdfUpdate]()

	def appendAll(updates: TraversableOnce[RdfUpdate]): Unit = {
		log.addAll(updates.toIterable.asJavaCollection)
	}

	def updates: Iterator[RdfUpdate] = log.iterator.asScala
	def updatesUpTo(time: Timestamp): Iterator[RdfUpdate] = ???
	def close(): Unit = {}
}