package fly.play.jiraExceptionProcessor

import org.specs2.mutable.{ Specification, Before }
import play.api.test._
import play.api.PlayException

object JiraExceptionProcessorSpec extends Specification with Before {
  def f = FakeApplication(new java.io.File("./src/test"))

  def before = play.api.Play.start(f)


  "Jira" should {
    "find the 'Hash' custom field name" in {
      val hashCustomField = Jira.hashCustomField
      (hashCustomField must not be empty) and
        (hashCustomField must startWith("customfield"))
    }
    "find the component id" in {
      val componentId = Jira.componentId
      componentId must not be empty
    }
    "find the project id" in {
      val projectId = Jira.projectId
      projectId must not be empty
    }
  }

  "JiraExceptionProcessor" should {
    "report an error and add a comment" in {

      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> Seq("headervalue"))), "body")

      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test"))

      ok
    }

    "send an email in case of an error while reporting" in {
      val e = Error(0, Seq("Dit is een test om te kijken of er een mailtje verstuurd wordt wanneer er iets mis gaat met de automatische error reporting", "[fake stack trace]"))
      JiraExceptionProcessor.sendEmail(e)
      ok
    }

    "report an error or add a comment with new line in summary" in {

      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> Seq("headervalue"))), "body")

      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with newline\nhere"))
      ok
    }

    "report an error or add a comment with weird exception information" in {

      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> Seq("headervalue"))), "body")

      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with more than 250 characters                                                                                                                                                                                                       end"))
      ok
    }

    "report an error or add a comment with weird exception information" in {

      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> Seq("headervalue"))), "body")

      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with more than 250 characters and a newline               \n                                                                                                                                                                                       end"))
      ok
    }

    "report an error or add a comment with an exceptionally large description" in {

      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> Seq("headervalue"))), "body")

      val thirtyThreeThousandSpaces = "                                                                                                    " * 330
      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with more than 32,767 characters in description" + thirtyThreeThousandSpaces + "end"))
      ok
    }
    
    "report an error or add a comment with an exceptionally large comment" in {

      val h = FakeHeaders(Seq("testheader" -> Range(1, 2000).map("headervalue" + _)))
      val r = FakeRequest("GET", "http://testuri.nl/?something", h, "body")

      JiraExceptionProcessor.reportError(r, new Exception("Issue from automatic test with more than 32,767 characters in comment"))
      ok
    }
    
    "report an error as similar if it's a PlayException" in {
      val r = FakeRequest("GET", "http://testuri.nl/?something", FakeHeaders(Seq("testheader" -> Seq("headervalue 1"))), "body 1")

      JiraExceptionProcessor.reportError(r, new PlayException("Issue from automatic test for play exception", "with ID"))
      ok
    }

    "report an error without a request" in {
      JiraExceptionProcessor.reportError(ErrorInformation("Issue for automatic test without request", "test description", "comment"))
      ok
    }

  }

  "ErrorInformation" should {

    "have an alternative apply method" in {
      val m = "test"
      val t = new Exception(m)
      val c = "comment"

      val e = ErrorInformation(t, c)

      e.summary === m
      e.description === JiraExceptionProcessor.getStackTraceString(t)
      e.comment === c
    }
  }
}
