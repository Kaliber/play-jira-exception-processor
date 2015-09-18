package fly.play.jiraExceptionProcessor

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ChildRestartStats
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategyConfigurator
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.libs.ws.WSClientConfig
import play.api.libs.ws.WSConfigParser
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.ning.NingWSClientConfigParser

class ReportingSupervisorStrategy extends SupervisorStrategyConfigurator {
  def create(): SupervisorStrategy =
    new ReportingStrategyWrapper(SupervisorStrategy.defaultStrategy, "Error during actor process")
}

class ReportingStrategyWrapper(wrapped: SupervisorStrategy, comment: String) extends SupervisorStrategy {

  def decider = wrapped.decider

  def handleChildTerminated(
    context: ActorContext,
    child: ActorRef,
    children: Iterable[ActorRef]
  ) = wrapped.handleChildTerminated(context, child, children)

  def processFailure(
    context: ActorContext,
    restart: Boolean,
    child: ActorRef,
    cause: Throwable,
    stats: ChildRestartStats,
    children: Iterable[ChildRestartStats]
  ) = {
    reportException(context, cause)
    wrapped.processFailure(context, restart, child, cause, stats, children)
  }

  private def reportException(context: ActorContext, exception: Throwable) = {
    val configuration = Configuration(context.system.settings.config)
    val wsClient = {
      val environment = Environment.simple(mode = Mode.Prod)
      val wsConfig = new WSConfigParser(configuration, environment).parse
      val ningWsConfig = new NingWSClientConfigParser(wsConfig, configuration, environment).parse
      NingWSClient(ningWsConfig)
    }

    import context.dispatcher

    new JiraExceptionProcessor(wsClient, configuration).reportError(ErrorInformation(exception, comment))
  }
}
