package net.kaliber.jiraExceptionProcessor

import com.typesafe.config.Config

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

  private val customFieldTypePath= "jira.exceptionProcessor.hashCustomFieldType"

  object FieldType extends Enumeration {
    val UUID, TEXT = Value

    def apply(s: String): Value =
      values.find(s.toLowerCase == _.toString.toLowerCase).getOrElse(UUID)
  }

  def fromConfig(config: Config): JiraExceptionProcessorConfiguration = {
    def error(key: String) = sys.error(s"Could not find $key in settings")

    def getString(key: String, default: Option[String] = None): String = {
      val configValueOption = if(config.hasPath(key)) {
        Some(config.getString(key))
      } else {
        default
      }

      configValueOption getOrElse error(key)
    }

    val hashCustomFieldType = if(config.hasPath(customFieldTypePath)) FieldType(config.getString(customFieldTypePath)) else FieldType.UUID

    JiraExceptionProcessorConfiguration(
      endpoint            = getString("jira.endpoint"),
      apiUsername         = getString("jira.username"),
      apiPassword         = getString("jira.password"),
      projectKey          = getString("jira.exceptionProcessor.projectKey"),
      componentName       = getString("jira.exceptionProcessor.componentName"),
      hashCustomFieldName = getString("jira.exceptionProcessor.hashCustomFieldName", default = Some("Hash")),
      hashCustomFieldType = hashCustomFieldType,
      issueType           = getString("jira.exceptionProcessor.issueType", default = Some("1")),
      fromName            = getString("jira.exceptionProcessor.mail.from.name"),
      fromAddress         = getString("jira.exceptionProcessor.mail.from.address"),
      toName              = getString("jira.exceptionProcessor.mail.to.name"),
      toAddress           = getString("jira.exceptionProcessor.mail.to.address")
    )
  }
}
