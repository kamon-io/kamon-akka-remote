package kamon.akka.instrumentation.kanela.advisor

import _root_.kanela.agent.libs.net.bytebuddy.asm.Advice._
import kamon.Kamon
import kamon.akka.context.ContextContainer
import kamon.context.Storage.Scope

/**
  * Advisor for akka.util.MessageBuffer.Node::constructor
  */
class MessageBufferNodeConstructorAdvisor
object MessageBufferNodeConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: ContextContainer): Unit = {
    node.context // forces initialization on the calling thread.
  }
}

/**
  * Advisor for akka.util.MessageBuffer.Node::apply
  */
class MessageBufferNodeMethodApplyAdvisor
object MessageBufferNodeMethodApplyAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@This node: ContextContainer): Scope = {
    Kamon.storeContext(node.context)
  }

  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: ContextContainer, @Enter scope: Scope): Unit = {
    scope.close()
  }
}