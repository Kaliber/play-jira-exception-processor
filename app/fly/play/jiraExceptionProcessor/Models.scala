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

  implicit class ReadsBuilder[M[_], A](o: M[A]) {
    def buildRead[B](f1: A => B)(implicit fu: Functor[M]) =
      fu.fmap[A, B](o, f1)
  }

  implicit class WritesBuilder[M[_], A](o: M[A]) {
    def buildWrite[B](f1: B => A)(implicit fu: ContravariantFunctor[M]) =
      fu.contramap[A, B](o, f1)
  }

  implicit val reads: Reads[PlayProjectIssue] =
    ((__ \ "key").readNullable[String] and
      (__ \ "fields").readNullable(
        (__ \ "summary").readNullable[String] and
          (__ \ "description").readNullable[String] and
          (__ \ Jira.hashCustomField).readNullable[String] tupled) tupled).map {
      case (key, Some((summary, description, customField))) =>
        PlayProjectIssue(key, summary, description, customField)
      case (key, None) => 
        PlayProjectIssue(key, None, None, None)
    }

  implicit val writes = {
    val specialUnapply = unlift(PlayProjectIssue.unapply) andThen {
      case (key, summary, description, hash) =>
        (
          Jira.projectId,
          summary,
          description,
          hash,
          Jira.issueType,
          Seq(Jira.componentId))
    }

    (__ \ "fields").write(
      (__ \ "project").write(
        (__ \ "id").write[String]) and
        (__ \ "summary").write[Option[String]] and
        (__ \ "description").write[Option[String]] and
        (__ \ Jira.hashCustomField).write[Option[String]] and
        (__ \ "issuetype").write(
          (__ \ "id").write[String]) and
          (__ \ "components").write(
            Writes.traversableWrites(
              (__ \ "id").write[String])) tupled) buildWrite specialUnapply
  }
}