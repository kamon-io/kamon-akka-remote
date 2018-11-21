package kamon.akka.instrumentation.kanela

import kamon.akka.instrumentation.kanela.advisor.{MessageBufferNodeConstructorAdvisor, MessageBufferNodeMethodApplyAdvisor}
import kamon.akka.instrumentation.kanela.mixin.HasTransientContextMixin
import kanela.agent.scala.KanelaInstrumentation

class MessageBufferInstrumentation extends KanelaInstrumentation {

  forTargetType("akka.util.MessageBuffer$Node") { builder ⇒
    builder
      .withMixin(classOf[HasTransientContextMixin])
      .withAdvisorFor(Constructor, classOf[MessageBufferNodeConstructorAdvisor])
      .withAdvisorFor(method("apply"), classOf[MessageBufferNodeMethodApplyAdvisor])
      .build()
  }

}
