package se.lu.nateko.cp.meta.services.labeling

import java.net.URI
import se.lu.nateko.cp.cpauth.core.UserId
import se.lu.nateko.cp.meta.mail.SendMail
import se.lu.nateko.cp.meta.utils.sesame._
import org.openrdf.model.Literal
import org.openrdf.model.{URI => SesameUri}
import se.lu.nateko.cp.meta.services.IllegalLabelingStatusException
import se.lu.nateko.cp.meta.services.UnauthorizedStationUpdateException

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try
import scala.util.Success
import scala.util.Failure

trait LifecycleService { self: StationLabelingService =>

	import LifecycleService._
	import AppStatus.AppStatus

	private val (factory, vocab) = getFactoryAndVocab(server)
	private val mailer = SendMail(config.mailing)

	def updateStatus(station: URI, newStatus: String, user: UserId)(implicit ctxt: ExecutionContext): Try[Unit] = {
		val stationUri = factory.createURI(station)

		for(
			toStatus <- lookupAppStatus(newStatus);
			fromStatus <- getCurrentStatus(stationUri);
			role <- getRoleForTransition(fromStatus, toStatus);
			_ <- if(userHasRole(user, role, stationUri))
					Success(())
				else
					Failure(new UnauthorizedStationUpdateException(s"User lacks the role of $role"))
		) yield {
			writeStatusChange(fromStatus, toStatus, stationUri)
			sendMailOnStatusUpdate(fromStatus, toStatus, stationUri, user)
		}

	}

	private def userHasRole(user: UserId, role: Role.Role, station: SesameUri): Boolean = {
		import Role._
		role match{
			case PI =>
				userIsPi(user, station)
			case TC =>
				val tcUsersListOpt = for(
					stationClass <- lookupStationClass(station);
					list <- config.tcUserIds.get(stationClass)
				) yield list
				tcUsersListOpt.toList.flatten.map(_.toLowerCase).contains(user.email.toLowerCase)
			case DG =>
				config.dgUserId.equalsIgnoreCase(user.email)

			case _ => false
		}
	}

	private def getCurrentStatus(station: SesameUri): Try[AppStatus] = {
		server.getStringValues(station, vocab.hasApplicationStatus)
			.headOption
			.map(lookupAppStatus)
			.getOrElse(Success(AppStatus.neverSubmitted))
	}

	private def writeStatusChange(from: AppStatus, to: AppStatus, station: SesameUri): Unit = {
		def toStatements(status: AppStatus) = Seq(factory
			.createStatement(station, vocab.hasApplicationStatus, vocab.lit(status.toString))
		)
		server.applyDiff(toStatements(from), toStatements(to))
	}

	private def sendMailOnStatusUpdate(
		from: AppStatus, to: AppStatus,
		station: SesameUri, user: UserId
	)(implicit ctxt: ExecutionContext): Unit = {

		if(to == AppStatus.submitted) Future{
			val recipients: Seq[String] = lookupStationClass(station)
					.flatMap(cls => config.tcUserIds.get(cls))
					.toSeq
					.flatten

			if(recipients.nonEmpty){
				val subject = "Application for labeling received"
				val stationId = lookupStationId(station).getOrElse("???")
				val body = views.html.LabelingEmail(user, stationId).body

				mailer.send(recipients, subject, body, Seq(user.email))
			}
		}
	}
}

object LifecycleService{

	object AppStatus extends Enumeration{
		type AppStatus = Value

		val neverSubmitted = Value("NEVER SUBMITTED")
		val notSubmitted = Value("NOT SUBMITTED")
		val submitted = Value("SUBMITTED")
		val acknowledged = Value("ACKNOWLEDGED")
		val approved = Value("APPROVED")
		val rejected = Value("REJECTED")
		val step2started = Value("STEP2STARTED")
		val step2approved = Value("STEP2APPROVED")
		val step2rejected = Value("STEP2REJECTED")
		val step3approved = Value("STEP3APPROVED")

	}

	object Role extends Enumeration{
		type Role = Value
		val PI = Value("Station PI")
		val TC = Value("ICOS TC representative")
		val DG = Value("ICOS DG")
	}

	import AppStatus._
	import Role._

	val transitions: Map[AppStatus, Map[AppStatus, Role]] = Map(
		neverSubmitted -> Map(submitted -> PI),
		submitted -> Map(acknowledged -> TC),
		acknowledged -> Map(
			notSubmitted -> TC,
			approved -> TC,
			rejected -> TC
		),
		notSubmitted -> Map(
			approved -> TC,
			rejected -> TC,
			submitted -> PI
		),
		approved -> Map(
			rejected -> TC,
			notSubmitted -> TC,
			step2started -> PI
		),
		rejected -> Map(
			approved -> TC,
			notSubmitted -> TC
		),
		step2started -> Map(
			approved -> TC,
			step2approved -> TC,
			step2rejected -> TC
		),
		step2approved -> Map(
			step2started -> TC,
			step2rejected -> TC,
			step3approved -> DG
		),
		step2rejected -> Map(
			step2started -> TC,
			step2approved -> TC
		),
		step3approved -> Map(step2approved -> DG)
	)

	private def getRoleForTransition(from: AppStatus, to: AppStatus): Try[Role] = {
		transitions.get(from) match{

			case None =>
				val message = s"No transitions defined from status: $from"
				Failure(new IllegalLabelingStatusException(message))

			case Some(destinations) =>
				destinations.get(to) match {

					case None =>
						val message = s"No transitions defined from $from to $to"
						Failure(new IllegalLabelingStatusException(message))

					case Some(role) =>
						Success(role)
				}
		}
	}

	private def lookupAppStatus(name: String): Try[AppStatus] = try{
		Success(AppStatus.withName(name))
	} catch{
		case nsee: NoSuchElementException =>
			val msg = s"Unsupported labeling application status '$name'"
			Failure(new IllegalLabelingStatusException(msg))
	}

}
