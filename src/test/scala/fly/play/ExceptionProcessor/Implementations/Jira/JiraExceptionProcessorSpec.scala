package fly.play.ExceptionProcessor.Implementations.Jira

import akka.actor.{Actor, ActorSystem, Props}
import akka.util.Timeout
import fly.play.ExceptionProcessor.ErrorInformation
import org.specs2.mutable.{Before, Specification}
import play.api.Play.current
import play.api._
import play.api.libs.ws.WS
import play.api.test._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration._

object JiraExceptionProcessorSpec extends Specification with Before {

  sequential

  def f = {
    val context = ApplicationLoader.createContext(
      Environment(new java.io.File("."), getClass.getClassLoader, Mode.Test)
    )
    ApplicationLoader(context) load context
  }

  def before = play.api.Play.start(f)

  for (i <- 1 to 2) {
    s"JiraExceptionProcessor $i" should {

      val jiraExceptionProcessor = new JiraExceptionProcessor(WS.client, current.configuration)

      "report an error and add a comment" in {

        val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> "headervalue")), "body")

        jiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test"))

        ok
      }

      "send an email in case of an error while reporting" in {
        val brokenConfiguration = current.configuration ++ Configuration(
          "jira.endpoint" -> "https://this-is-wrong.atlassian.net/rest/api/2/"
        )

        val jiraExceptionProcessor = new JiraExceptionProcessor(WS.client, brokenConfiguration)
        val e = ErrorInformation("Dit is een test om te kijken of er een mailtje verstuurd wordt wanneer er iets mis gaat met de automatische error reporting", "test description", "[fake stack trace]")
        jiraExceptionProcessor.reportError(e)
        ok
      }

      "report an error or add a comment with new line in summary" in {

        val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> "headervalue")), "body")

        jiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with newline\nhere"))
        ok
      }

      "report an error or add a comment with weird exception information" in {

        val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> "headervalue")), "body")

        jiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with more than 250 characters                                                                                                                                                                                                       end"))
        ok
      }

      "report an error or add a comment with weird exception information" in {

        val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> "headervalue")), "body")

        jiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with more than 250 characters and a newline               \n                                                                                                                                                                                       end"))
        ok
      }

      "report an error as similar if it's a PlayException" in {
        val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> "headervalue 1")), "body 1")

        jiraExceptionProcessor.reportError(r, new PlayException("Issue from automatic test for play exception", "with ID"))
        ok
      }

      "report an error without a request" in {
        jiraExceptionProcessor.reportError(ErrorInformation("Issue for automatic test without request", "test description", "comment"))
        ok
      }

      "report problem in akka actor" in {
        class TestActor extends Actor {
          def receive = {
            case "test" => sys error "test actor failure"
            case "done" => sender ! "done"
          }
        }
        val system = ActorSystem("test")
        val ref = system.actorOf(Props(new TestActor))

        ref ! "test"

        import akka.pattern.ask
        implicit val timeout = Timeout(5.seconds)
        Await.result(ref ? "done", timeout.duration)

        system.shutdown()
        system.awaitTermination()
        ok
      }
    }
  }
}
