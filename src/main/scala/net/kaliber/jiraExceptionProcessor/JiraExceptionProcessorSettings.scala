package net.kaliber.jiraExceptionProcessor

import com.typesafe.config.Config
import net.kaliber.mailer.Session

case class JiraExceptionProcessorSettings(
  enabled: Boolean,
  config: Config
) {
  def processorSettings: JiraExceptionProcessorConfiguration =
    JiraExceptionProcessorConfiguration.fromConfig(config)

  def mailerSession: Session = Session.fromConfig(config)
}

object JiraExceptionProcessorSettings {
  def fromConfig(config: Config) = {
    val enabledPath = "jira.exceptionProcessor.enabled"
    val isEnabled = if(config.hasPath(enabledPath)) config.getBoolean(enabledPath) else true

    JiraExceptionProcessorSettings(
      isEnabled,
      config
    )
  }
}
