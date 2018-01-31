package kamon.akka.instrumentation

import akka.actor._
import akka.remote.RemoteScope
import kamon.Kamon
import kamon.testkit.ContextTesting

class TraceTokenReplier(creationTraceContextListener: Option[ActorRef]) extends Actor with ActorLogging with ContextTesting {
  creationTraceContextListener foreach { recipient ⇒
    recipient ! currentTraceContextInfo
  }

  def receive = {
    case "die" ⇒
      throw new ArithmeticException("Division by zero.")
    case "reply-trace-token" ⇒
      sender ! currentTraceContextInfo
  }

  def currentTraceContextInfo: String = {
    val ctx = Kamon.currentContext()
    val name = ctx.get(StringBroadcastKey).getOrElse("")
    s"name=$name"
  }
}

object TraceTokenReplier {
  def props(creationTraceContextListener: Option[ActorRef]): Props =
    Props(classOf[TraceTokenReplier], creationTraceContextListener)

  def remoteProps(creationTraceContextListener: Option[ActorRef], remoteAddress: Address): Props = {
    Props(classOf[TraceTokenReplier], creationTraceContextListener)
      .withDeploy(Deploy(scope = RemoteScope(remoteAddress)))

  }
}
