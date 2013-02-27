package fly.play.jiraExceptionProcessor

import play.api.Play.current
import play.api.Application
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.functional.InvariantFunctor
import play.api.libs.functional.Functor
import play.api.libs.functional.ContravariantFunctor
import scala.language.higherKinds
import scala.language.postfixOps

case class Error(status: Int, messages: Seq[String])

object Error {
  def fromJson(status: Int, json: JsValue) =
    Error(status,
      (json \ "errorMessages").as[Seq[String]] ++
        (json \ "errors").asOpt[Map[String, String]].toSeq.flatten.map {
          case (key, value) => s"$key: $value"
        })
}

trait Success
object Success extends Success

case class PlayProjectIssue(
  key: Option[String],
  summary: Option[String],
  description: Option[String],
  hash: Option[String]) {

}

object PlayProjectIssue extends ((Option[String], Option[String], Option[String], Option[String]) => PlayProjectIssue) {

   implicit object format extends Format[PlayProjectIssue] {
    def reads(json: JsValue) = {

      val fields = json \ "fields"

      JsSuccess(PlayProjectIssue(
        (json \ "key").asOpt[String],
        (fields \ "summary").asOpt[String],
        (fields \ "description").asOpt[String],
        (fields \ Jira.hashCustomField).asOpt[String]))
    }

    def writes(playProjectIssue: PlayProjectIssue) = {

      def field(pairs: (String, String)*): JsObject =
        JsObject(pairs.map { case (key, value) => key -> toJson(value) })
      def map(pairs: (String, JsValue)*): JsObject =
        JsObject(pairs)

      map(
        "fields" -> map(
          "project" -> field("id" -> Jira.projectId),
          "summary" -> toJson(playProjectIssue.summary),
          "description" -> toJson(playProjectIssue.description),
          "issuetype" -> field("id" -> Jira.issueType),
          "components" -> JsArray(Seq(field("id" -> Jira.componentId))),
          Jira.hashCustomField -> toJson(playProjectIssue.hash)))
    }
  }
}