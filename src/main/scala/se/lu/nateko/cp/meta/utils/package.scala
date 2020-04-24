package se.lu.nateko.cp.meta

import java.time.Instant
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import scala.util.{Try, Success, Failure}

package object utils {

import scala.reflect.ClassTag

	implicit class ToTryConvertibleOption[T](val inner: Option[T]) extends AnyVal{
		def toTry(error: => Throwable): Try[T] = inner.map(Success.apply)
			.getOrElse(Failure(error))
	}

	def urlEncode(s: String): String = {
		new java.net.URI(null, null, "/" + s, null).toASCIIString.substring(1)
	}

	def parseInstant(dateTimeIso: String): Instant = Instant.from(ISO_DATE_TIME.parse(dateTimeIso))

	def getStackTrace(err: Throwable): String = {
		val traceWriter = new java.io.StringWriter()
		err.printStackTrace(new java.io.PrintWriter(traceWriter))
		traceWriter.toString
	}

	implicit class OptionalItemOrSeqOps[T](val item: Option[Either[T, Seq[T]]]) extends AnyVal{
		def flattenToSeq: Seq[T] = item.fold(Seq.empty[T]){either =>
			either.fold(Seq(_), identity)
		}
	}

	implicit class AnyRefWithSafeOptTypecast(val inner: AnyRef) extends AnyVal{
		def asOptInstanceOf[T: ClassTag]: Option[T] = inner match{
			case t: T => Some(t)
			case _ => None
		}
	}
}
