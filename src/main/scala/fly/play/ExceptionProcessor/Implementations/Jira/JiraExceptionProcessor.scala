package fly.play.ExceptionProcessor.Implementations.Jira

import java.io.{PrintWriter, StringWriter}
import javax.mail.Message

import fly.play.ExceptionProcessor.{Error, ErrorInformation, ExceptionProcessor, PlayProjectIssue}
import net.kaliber.mailer._
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import play.api.{Configuration, Logger, PlayException}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class JiraExceptionProcessor(val client: WSClient, val configuration: Configuration)(implicit ec: ExecutionContext) extends ExceptionProcessor {


  // these are lazy so the configuration can have missing settings when disabled
  private lazy val processorConfiguration = JiraExceptionProcessorConfiguration fromConfiguration configuration
  private lazy val (jira, mailer) = (
    new Jira(client, processorConfiguration),
    new Mailer(Session fromConfiguration configuration)
  )

  /*
   * Public functions do not return a Future. Implementers might forget
   * to process the result of the future and with that fail to catch any
   * reporting problems.
   */

  def reportError(request: RequestHeader, ex: Throwable): Unit =
    reportError(ErrorInformation(ex, getRequestString(request)))

  def reportError(information: ErrorInformation): Unit =
    Await.result(actualReport(information), 10.seconds)

  private def actualReport(information: ErrorInformation): Future[Unit] = {
    val enabled = configuration.getBoolean("jira.exceptionProcessor.enabled").getOrElse(true)

    Logger.error(s"Logged from Jira exception processor (enabled = $enabled)")
    Logger.error("Summary: "       + information.summary)
    Logger.error("Description: \n" + information.description)
    Logger.error("Comment: \n"     + information.comment)

    if (!enabled) return Future.successful(())

    val result =
      reportToJira(information)
        .recover {
          case e: PlayException => throw e
          case t: Throwable     => Left(toError(t, information))
        }

    result flatMap {
      case Left(error) => sendEmail(error)
      case Right(success) =>
        /* error reported */
        Future.successful(())
    }
  }

  private def toError(t: Throwable, information: ErrorInformation) = {
    val ErrorInformation(summary, description, comment) = information

    Error(0,
      Seq(
        "Exception while calling Jira:",
        t.getMessage,
        JiraExceptionProcessor.getStackTraceString(t),
        "Original error:",
        summary,
        description,
        comment
      )
    )
  }

  private def reportToJira(information: ErrorInformation) = {
    import JiraExceptionProcessor.Helper

    val ErrorInformation(summary, description, comment) = information
    val hash = information.hash

    val Helper(result) =
      for {
        optionalIssue <- Helper(jira.findIssues(hash))
        issue         <-
          optionalIssue match {
            case Some(issue) => Helper(issue)
            case None        =>
              Helper(jira createIssue PlayProjectIssue(summary, description, hash))
          }
        _             <- Helper(jira.addComment(issue.key.get, information.comment))
      } yield ()

    result
  }

  private def getRequestString(request: RequestHeader): String = {

    "uri: " + request.uri + "\n" +
      "path: " + request.path + "\n" +
      "method: " + request.method + "\n" +
      "headers: \n" +
      request.headers.toMap.toList.map((keyValueSeq _).tupled).mkString("\n") + "\n" +
      "session: \n" +
      request.session.data.toList.map((keyValue _).tupled).mkString("\n") + "\n" +
      "flash: \n" +
      request.flash.data.toList.map((keyValue _).tupled).mkString("\n")

  }

  private def keyValue(key: String, value: String): String = "   " + key + ": " + value
  private def keyValueSeq(key: String, value: Seq[String]): String = keyValue(key, value.mkString(", "))

  private def sendEmail(error: Error):Future[Unit] = {
    val message = s"""|Status: ${error.status}
                      |${error.messages.mkString("\n\n")}""".stripMargin

    Logger.error("Failed to report to Jira, message: " + message)

    mailer.sendEmail(
      Email(
        subject = s"Failed to report error for project ${processorConfiguration.projectKey} and component ${processorConfiguration.componentName}",
        from = EmailAddress(processorConfiguration.fromName, processorConfiguration.fromAddress),
        replyTo = None,
        recipients = List(
          Recipient(Message.RecipientType.TO, EmailAddress(processorConfiguration.toName, processorConfiguration.toAddress))
        ),
        text = message,
        htmlText = message.replace("\n", "<br />"),
        attachments = Seq.empty
      )
    )
  }
}

object JiraExceptionProcessor {
  def getStackTraceString(ex: Throwable): String = {
    val s = new StringWriter
    val p = new PrintWriter(s)
    ex.printStackTrace(p)
    s.toString
  }

  /**
   * This wrapped is used to be able to map and flatMap on a more complex type
   */
  private case class Helper[A](underlying: Future[Either[Error, A]]) {
    def flatMap[B](f: A => Helper[B])(implicit ec: ExecutionContext): Helper[B] =
      Helper(
        underlying.flatMap {
          case Right(a) => f(a).underlying
          case Left(error) => Future successful Left(error)
        }
      )

    def map[B](f: A => B)(implicit ec: ExecutionContext): Helper[B] =
      flatMap(a => Helper(f(a)))
  }

  private object Helper {
    def apply[A](a: A): Helper[A] = Helper(Future successful Right(a))
  }
}

