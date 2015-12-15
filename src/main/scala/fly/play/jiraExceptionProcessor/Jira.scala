package fly.play.jiraExceptionProcessor

import play.api.Play.current
import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJson
import play.api.libs.ws.WSAuthScheme
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class Jira(
  client: WSClient,
  configuration: JiraExceptionProcessorConfiguration
)(implicit ec: ExecutionContext) {

  /**
   * Retrieves a list of issues for the given hash
   */
  def findIssues(hash: String): Future[Either[Error, Option[PlayProjectIssue]]] =
    for {
      componentId     <- componentId
      hashCustomField <- hashCustomField
      result          <- findIssue(hash, componentId, hashCustomField)
    } yield result

  def createIssue(issue: PlayProjectIssue): Future[Either[Error, PlayProjectIssue]] =
    playProjectIssueFormat flatMap { implicit format =>
      val body = toJson(issue)

      request("issue").post(body) map handleResponse {
        case (201, response) => Right(response.json.as[PlayProjectIssue])
      }
    }

  /**
   * Simple method to add a comment to an issue
   */
  def addComment(issueKey: String, comment: String): Future[Either[Error, Unit]] = {
    val body = toJson(Map("body" -> toJson(comment)))

    request(s"issue/$issueKey/comment")
      .post(body) map handleResponse {
        case (201, _) => Right(())
      }
  }

  private def findIssue(hash: String, componentId: String, hashCustomField: String) =
    playProjectIssueFormat flatMap { implicit format =>
      request("search")
        .withQueryString(
          "jql" -> (
             s"project = ${configuration.projectKey} AND " +
             s"component = $componentId AND " +
             s"resolution = Unresolved AND " +
             s"${configuration.hashCustomFieldName} = $hash " +
             "ORDER BY priority DESC"
          ),
          "fields" -> s"summary,key,description,$hashCustomField"
        )
        .get() map handleResponse {
          case (200, response) => {
            Right((response.json \ "issues").as[Seq[PlayProjectIssue]].headOption)
          }
        }
    }

  private def playProjectIssueFormat =
    Future.sequence(Seq(projectId, componentId, hashCustomField)) map {
      case Seq(projectId, componentId, hashCustomField) =>
        PlayProjectIssue.format(projectId, componentId, hashCustomField, configuration.issueType)
    }

  private lazy val hashCustomField =
    // request a list of all fields
    request("field").get() map { response =>
      (response.status, response) match {
        case (200, response) =>
          extractHashCustomFieldIdFrom(response.json)
        case (status, response) =>
          throw new Exception("Problem retrieving the 'Hash' custom field ('%s'): %s" format (status, response.body))
      }
    }

  private def extractHashCustomFieldIdFrom(json: JsValue) = {
    val hashCustomField =
      json.as[Seq[JsValue]]
        .find { field =>
          (field \ "name").as[String] == configuration.hashCustomFieldName
        }
        .getOrElse(throw new Exception("Could not find a field with the name 'Hash'"))

    (hashCustomField \ "id").as[String]
  }

  private lazy val project =
    request("project/%s" format configuration.projectKey)
      .get() map { response =>
        (response.status, response) match {
          case (200, response) => response.json
          case (status, response) =>
            throw new Exception("Problem retrieving the 'Hash' custom field ('%s'): %s" format (status, response.body))
        }
      }

  private lazy val componentId =
    for {
      project     <- project
      components  =  (project \ "components").as[Seq[JsValue]]
      component   =  findComponentIn(components)
      componentId =  (component \ "id").as[String]
    } yield componentId

  private def findComponentIn(components: Seq[JsValue]) =
    components
      .find { component =>
        (component \ "name").as[String] == configuration.componentName
      }
      .getOrElse {
        sys.error("Problem retrieving the id of the component '%s', component not found" format configuration.componentName)
      }

  private lazy val projectId = project.map(project => (project \ "id").as[String])


  /**
   * Utility method that is used to perform requests
   */
  private def request(relativePath: String) = {
    val completeUrl = configuration.endpoint + relativePath

    client
      .url(completeUrl)
      .withAuth(configuration.apiUsername, configuration.apiPassword, WSAuthScheme.BASIC)
  }

  /**
   * Utility method that will handle errors and unknown status codes
   */
  private def handleResponse[T](handler: PartialFunction[(Int, WSResponse), Either[Error, T]])(response: WSResponse): Either[Error, T] = {
    val defaultHandler: PartialFunction[(Int, WSResponse), Either[Error, T]] = {
      case (400, response) => Left(Error.fromJson(400, response.json))
      case (other, response) => Left(Error(other, Seq("Unknown error", response.body)))
    }

    (handler orElse defaultHandler)(response.status, response)
  }
}
