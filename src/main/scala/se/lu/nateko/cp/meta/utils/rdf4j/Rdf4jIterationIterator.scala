package se.lu.nateko.cp.meta.utils.rdf4j

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.AbstractIterator

import org.eclipse.rdf4j.common.iteration.CloseableIteration

import se.lu.nateko.cp.meta.api.CloseableIterator


class Rdf4jIterationIterator[T](res: CloseableIteration[T, _], closer: () => Unit = () => ()) extends AbstractIterator[T] with CloseableIterator[T]{

	private[this] val closed = new AtomicBoolean()

	def close(): Unit = if(!closed.getAndSet(true)){
		try{
			res.close()
		} finally {
			closer()
		}
	}

	def hasNext: Boolean = !closed.get && {
		try{
			val has = res.hasNext()
			if(!has) close()
			has
		}
		catch{
			case err: Throwable =>
				close()
				throw err
		}
	}

	def next(): T =
		try{
			res.next()
		}
		catch{
			case err: Throwable =>
				close()
				throw err
		}

	override protected def finalize(): Unit = {
		close()
		super.finalize()
	}
}
