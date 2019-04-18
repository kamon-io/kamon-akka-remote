package kamon.akka.instrumentation.kanela

import kamon.akka.instrumentation.kanela.advisor.{MessageBufferNodeConstructorAdvisor, MessageBufferNodeMethodApplyAdvisor}
import kamon.akka.instrumentation.kanela.mixin.HasTransientContextMixin
import kanela.agent.scala.KanelaInstrumentation

class MessageBufferInstrumentation extends KanelaInstrumentation with AkkaVersionedFilter {
  /**
    * Instrument:
    *
    * akka.util.MessageBuffer$Node::constructor
    * akka.util.MessageBuffer$Node::apply
    *
    * Mix:
    *
    * akka.util.MessageBuffer$Node with kamon.akka.instrumentation.kanela.mixin.HasTransientContextMixin
    *
    */
  forTargetType("akka.util.MessageBuffer$Node") { builder ⇒
    filterAkkaVersion(builder)
      .withMixin(classOf[HasTransientContextMixin])
      .advise(Constructor, classOf[MessageBufferNodeConstructorAdvisor])
      .advise(method("apply"), classOf[MessageBufferNodeMethodApplyAdvisor])
      .build()
  }

}
