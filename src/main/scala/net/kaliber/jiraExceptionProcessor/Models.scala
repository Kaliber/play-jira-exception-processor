package net.kaliber.jiraExceptionProcessor

import play.api.libs.Codecs
import play.api.libs.json.Json.toJson
import play.api.libs.json._

case class Error(status: Int, messages: Seq[String])

object Error {
  def fromJson(status: Int, json: JsValue) =
    Error(status,
      (json \ "errorMessages").as[Seq[String]] ++
        (json \ "errors").asOpt[Map[String, String]].toSeq.flatten.map {
          case (key, value) => s"$key: $value"
        })
}

case class ProjectIssue(
  key: Option[String],
  summary: Option[String],
  description: Option[String],
  hash: Option[String]) {
}

object ProjectIssue extends ((Option[String], Option[String], Option[String], Option[String]) => ProjectIssue) {

  def apply(summary: String, description: String, hash: String): ProjectIssue =
    ProjectIssue(None, Some(summary), Some(description), Some(hash))

  def format(
    projectId: String,
    componentId: String,
    hashCustomFieldName: String,
    issueType: String
  ): Format[ProjectIssue] = new Format[ProjectIssue] {

    def reads(json: JsValue) = {

      val fields = json \ "fields"

      JsSuccess(ProjectIssue(
        (json \ "key").asOpt[String],
        (fields \ "summary").asOpt[String],
        (fields \ "description").asOpt[String],
        (fields \ hashCustomFieldName).asOpt[String]))
    }

    def writes(projectIssue: ProjectIssue) = {

      def field[T : Writes](pairs: (String, T)*): JsObject =
        JsObject(pairs.map { case (key, value) => key -> toJson(value) })
      def map(pairs: (String, JsValue)*): JsObject =
        JsObject(pairs)

      map(
        "fields" -> map(
          "project" -> field("id" -> projectId),
          "summary" -> toJson(trimSummary(projectIssue.summary)),
          "description" -> toJson(
            projectIssue.summary.getOrElse("") + "\n" +
              projectIssue.description.getOrElse("")
          ),
          "issuetype" -> field("id" -> issueType),
          "components" -> JsArray(Seq(field("id" -> componentId))),
          hashCustomFieldName -> toJson(projectIssue.hash)))
    }

    private def trimSummary(summary: Option[String]): Option[String] =
      summary.map(_.takeWhile(_ != '\n').take(250))
  }
}

case class ErrorInformation(summary: String, description: String, comment: String) {

  lazy val hash = createHash(removePlayId(description))

  private def removePlayId(message: String) =
    message.replaceFirst("""@[^\s]*""", "")

  private def createHash(str: String): String =
    Codecs.md5(str.getBytes)
}

object ErrorInformation extends ((String, String, String) => ErrorInformation) {

  def apply(ex: Throwable, comment: String): ErrorInformation =
    ErrorInformation(extractMessage(ex), JiraExceptionProcessor.getStackTraceString(ex), comment)

  private def extractMessage(ex: Throwable): String =
    ex.getMessage + Option(ex.getCause).map("\n=== Caused by ===\n" + extractMessage(_)).getOrElse("")
}
