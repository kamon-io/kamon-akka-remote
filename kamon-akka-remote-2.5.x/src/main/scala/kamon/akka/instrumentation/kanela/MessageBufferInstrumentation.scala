package kamon.akka.instrumentation.kanela

import kamon.akka.instrumentation.kanela.advisor.{MessageBufferNodeConstructorAdvisor, MessageBufferNodeMethodApplyAdvisor}
import kamon.instrumentation.akka25.mixin.HasTransientContextMixin
import kanela.agent.api.instrumentation.InstrumentationBuilder

class MessageBufferInstrumentation extends InstrumentationBuilder {

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
  onType("akka.util.MessageBuffer$Node")
    .mixin(classOf[HasTransientContextMixin])
    .advise(isConstructor, classOf[MessageBufferNodeConstructorAdvisor])
    .advise(method("apply"), classOf[MessageBufferNodeMethodApplyAdvisor])

}
