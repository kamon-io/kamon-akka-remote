package kamon.akka.instrumentation.kanela.advisor

import _root_.kanela.agent.libs.net.bytebuddy.asm.Advice._
import kamon.Kamon
import kamon.context.Storage.Scope
import kamon.instrumentation.Mixin.HasContext

/**
  * Advisor for akka.util.MessageBuffer.Node::constructor
  */
class MessageBufferNodeConstructorAdvisor

object MessageBufferNodeConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: HasContext): Unit = {
    node.context // forces initialization on the calling thread.
  }
}

/**
  * Advisor for akka.util.MessageBuffer.Node::apply
  */
class MessageBufferNodeMethodApplyAdvisor

object MessageBufferNodeMethodApplyAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@This node: HasContext): Scope = {
    Kamon.storeContext(node.context)
  }

  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: HasContext, @Enter scope: Scope): Unit = {
    scope.close()
  }
}