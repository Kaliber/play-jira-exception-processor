package fly.play.jiraExceptionProcessor

import play.api.Play.current
import play.api.libs.json.{ Format, JsObject }
import play.api.libs.json.Json.toJson
import play.api.libs.json.Reads
import play.api.libs.ws.{ WSResponse, WS, WSAuthScheme }
import play.api.libs.json.JsValue
import play.Logger
import play.api.Application
import play.modules.mailer.PlayConfiguration
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import play.api.libs.json.Json

object Jira {
  /**
   * The endpoint, for example: https://jira.rhinofly.net/rpc/json-rpc/jirasoapservice-v2/
   */
  val endpoint = PlayConfiguration("jira.endpoint")
  val apiUsername = PlayConfiguration("jira.username")
  val apiPassword = PlayConfiguration("jira.password")

  /**
   * Fields needed to create and retrieve issues
   */
  val projectKey = PlayConfiguration("jira.exceptionProcessor.projectKey")
  val componentName = PlayConfiguration("jira.exceptionProcessor.componentName")
  val configuration = implicitly[Application].configuration
  val hashCustomFieldName =
    configuration
      .getString("jira.exceptionProcessor.hashCustomFieldName")
      .getOrElse("Hash")
  val issueType =
    configuration
      .getString("jira.exceptionProcessor.issueType")
      .getOrElse("1")

  /**
   * Searches for a custom field with the name hash
   */
  lazy val hashCustomField = {
    val customFieldPromise =
      // request a list of all fields
      request("field").get() map { response =>

        (response.status, response) match {

          case (200, response) => {
            // retrieve the custom field from the list of fields
            val hashCustomField =
              response.json.as[Seq[JsValue]]
                .filter(field => (field \ "name").as[String] == "Hash")
                .headOption
                .getOrElse(throw new Exception("Could not find a field with the name 'Hash'"))

            (hashCustomField \ "id").as[String]
          }
          case (status, response) =>
            throw new Exception("Problem retrieving the 'Hash' custom field ('%s'): %s" format (status, response.body))
        }
      }

    Await.result(customFieldPromise, 10 seconds)
  }

  /**
   * Retrieves the project
   */
  lazy val project = {
    val projectPromise =
      request("project/%s" format projectKey)
        .get() map { response =>
          (response.status, response) match {
            case (200, response) => response.json
            case (status, response) =>
              throw new Exception("Problem retrieving the 'Hash' custom field ('%s'): %s" format (status, response.body))
          }
        }
    Await.result(projectPromise, 10 seconds)
  }

  /**
   * Determines the correct component id
   */
  lazy val componentId = {
    val components = (project \ "components").as[Seq[JsValue]]

    val foundComponents = for {
      component <- components
      if ((component \ "name").as[String] == componentName)
    } yield (component \ "id").as[String]

    foundComponents
      .headOption
      .getOrElse(throw new Exception("Problem retrieving the id of the component '%s', component not found" format componentName))
  }

  lazy val projectId = (project \ "id").as[String]


  /**
   * Utility method that is used to perform requests
   */
  protected def request(relativePath: String) = {
    val completeUrl = endpoint + relativePath

    Logger info ("Complete URL: %s" format completeUrl)

    WS
      .url(completeUrl)
      .withAuth(apiUsername, apiPassword, WSAuthScheme.BASIC)
  }

  /**
   * Utility method that will handle errors and unknown status codes
   */
  def handleResponse[T](handler: PartialFunction[(Int, WSResponse), Either[Error, T]])(response: WSResponse): Either[Error, T] = {
    val defaultHandler: PartialFunction[(Int, WSResponse), Either[Error, T]] = {
      case (400, response) => Left(Error.fromJson(400, response.json))
      case (other, response) => Left(Error(other, Seq("Unknown error", response.body)))
    }

    (handler orElse defaultHandler)(response.status, response)
  }

  /**
   * Simple method to add a comment to an issue
   */
  def addComment(issueKey: String, comment: String): Future[Either[Error, Success]] = {
    val body = toJson(Map("body" -> toJson(comment)))

    request("issue/%s/comment" format issueKey)
      .post(body) map handleResponse {
        case (201, _) => Right(Success)
      }
  }

  /**
   * Retrieves a list of issues for the given hash
   */
  def findIssues(hash: String): Future[Either[Error, Option[PlayProjectIssue]]] = {
    request("search")
      .withQueryString(
        "jql" ->
          ("project = %s AND component = %s AND resolution = Unresolved AND %s = %s ORDER BY priority DESC"
            .format(projectKey, componentId, hashCustomFieldName, hash)),
        "fields" -> ("summary,key,description,%s" format hashCustomField))

      .get() map handleResponse {
        case (200, response) => {
          Right((response.json \ "issues").as[Seq[PlayProjectIssue]].headOption)
        }
      }
  }

  def createIssue(issue: PlayProjectIssue): Future[Either[Error, PlayProjectIssue]] = {
    val body = toJson(issue)

    request("issue").post(body) map handleResponse {
      case (201, response) => Right(response.json.as[PlayProjectIssue])
    }
  }

}
