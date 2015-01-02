package fly.play.jiraExceptionProcessor

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ChildRestartStats
import akka.actor.SupervisorStrategy
import akka.actor.SupervisorStrategy.Decider
import akka.actor.SupervisorStrategyConfigurator

class ReportingSupervisorStrategy extends SupervisorStrategyConfigurator {
  def create(): SupervisorStrategy = new ReportingStrategyWrapper(SupervisorStrategy.defaultStrategy, "Error during actor process")
}

class ReportingStrategyWrapper(that: SupervisorStrategy, comment: String) extends SupervisorStrategy {
  def decider: Decider = {
    case ex: Exception =>
      JiraExceptionProcessor.reportError(ErrorInformation(ex, comment))
      that.decider(ex)
  }

  def handleChildTerminated(
    context: ActorContext,
    child: ActorRef,
    children: Iterable[ActorRef]): Unit = that.handleChildTerminated(context, child, children)
  def processFailure(
    context: ActorContext,
    restart: Boolean,
    child: ActorRef,
    cause: Throwable,
    stats: ChildRestartStats,
    children: Iterable[ChildRestartStats]): Unit = that.processFailure(context, restart, child, cause, stats, children)
}
