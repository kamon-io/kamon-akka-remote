package akka.remote.kamon.instrumentation.kanela.advisor

import _root_.kanela.agent.libs.net.bytebuddy.asm.Advice._
import akka.dispatch.sysmsg.SystemMessage
import kamon.Kamon
import kamon.akka.context.ContextContainer
import kamon.context.Storage.Scope
import kamon.instrumentation.Mixin.HasContext
import akka.remote.EndpointManager.Send

/**
  * Advisor for akka.remote.EndpointManager.Send::constructor
  */
class SendConstructorAdvisor
object SendConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: HasContext): Unit = {
    node.context // forces initialization on the calling thread.
  }
}

/**
  * Advisor for akka.remote.EndpointWriter::writeSend
  */
class EndpointWriterWriteSendMethodAdvisor
object EndpointWriterWriteSendMethodAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@Argument(0) send: Send): Scope = {
    Kamon.storeContext(send.asInstanceOf[HasContext].context)
  }

  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@Enter scope: Scope): Unit = {
    scope.close()
  }
}


/**
  * Advisor for akka.actor.ActorCell::sendSystemMessage
  * Advisor for akka.actor.UnstartedCell::sendSystemMessage
  */
class SendSystemMessageMethodAdvisor
object SendSystemMessageMethodAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@Argument(0) msg: SystemMessage): Unit = {
    msg.asInstanceOf[ContextContainer].setContext(Kamon.currentContext())
  }
}

