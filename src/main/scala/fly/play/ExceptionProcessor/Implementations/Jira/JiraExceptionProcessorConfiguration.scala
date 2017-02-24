package fly.play.ExceptionProcessor.Implementations.Jira

import play.api.{Configuration, PlayException}

case class JiraExceptionProcessorConfiguration(
  endpoint: String,
  apiUsername: String,
  apiPassword: String,
  projectKey: String,
  componentName: String,
  hashCustomFieldName: String,
  hashCustomFieldType: JiraExceptionProcessorConfiguration.FieldType.Value,
  issueType: String,
  fromName: String,
  fromAddress: String,
  toName: String,
  toAddress: String
)

object JiraExceptionProcessorConfiguration {

  object FieldType extends Enumeration {
    val UUID, TEXT = Value

    def apply(s: String): Value =
      values.find(s.toLowerCase == _.toString.toLowerCase).getOrElse(UUID)
  }

  def fromConfiguration(configuration: Configuration): JiraExceptionProcessorConfiguration = {

    def getString(key: String, default: Option[String] = None): String =
      configuration getString key orElse default getOrElse error(key)

    JiraExceptionProcessorConfiguration(
      endpoint            = getString("jira.endpoint"),
      apiUsername         = getString("jira.username"),
      apiPassword         = getString("jira.password"),
      projectKey          = getString("jira.exceptionProcessor.projectKey"),
      componentName       = getString("jira.exceptionProcessor.componentName"),
      hashCustomFieldName = getString("jira.exceptionProcessor.hashCustomFieldName", default = Some("Hash")),
      hashCustomFieldType = configuration.getString("jira.exceptionProcessor.hashCustomFieldType").map(FieldType(_)).getOrElse(FieldType.UUID),
      issueType           = getString("jira.exceptionProcessor.issueType", default = Some("1")),
      fromName            = getString("jira.exceptionProcessor.mail.from.name"),
      fromAddress         = getString("jira.exceptionProcessor.mail.from.address"),
      toName              = getString("jira.exceptionProcessor.mail.to.name"),
      toAddress           = getString("jira.exceptionProcessor.mail.to.address")
    )
  }

  private def error(key: String) = throw new PlayException("Configuration error", s"Could not find $key in settings")
}
