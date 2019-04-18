package kamon.akka.instrumentation.kanela

import akka.kamon.instrumentation.kanela.interceptor.AkkaPduProtobufCodecConstructMessageMethodInterceptor
import akka.remote.kamon.instrumentation.kanela.advisor._
import kamon.akka25.instrumentation.kanela.mixin.HasTransientContextMixin
import kanela.agent.api.instrumentation.InstrumentationBuilder

class RemotingInstrumentation extends InstrumentationBuilder {
  /**
    * Instrument:
    *
    * akka.remote.EndpointManager$Send::constructor
    *
    * Mix:
    *
    * akka.remote.EndpointManager$Send with kamon.akka.instrumentation.kanela.mixin.HasTransientContextMixin
    *
    */
  onType("akka.remote.EndpointManager$Send")
    .mixin(classOf[HasTransientContextMixin])
    .advise(isConstructor, classOf[SendConstructorAdvisor])

  /**
    * Instrument:
    *
    * akka.remote.EndpointWriter::writeSend
    *
    */
  onType("akka.remote.EndpointWriter")
    .advise(method("writeSend"), classOf[EndpointWriterWriteSendMethodAdvisor])

  /**
    * Instrument:
    *
    * akka.actor.ActorCell::sendSystemMessage
    *
    */
  onType("akka.actor.ActorCell")
    .advise(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])

  /**
    * Instrument:
    *
    * akka.actor.UnstartedCell::sendSystemMessage
    *
    */
  onType("akka.actor.UnstartedCell")
    .advise(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])


  /**
    * Instrument:
    *
    * akka.remote.transport.AkkaPduProtobufCodec$::constructMessage
    * akka.remote.transport.AkkaPduProtobufCodec$::decodeMessage
    *
    */
  onType("akka.remote.transport.AkkaPduProtobufCodec$")
    .intercept(method("constructMessage"), classOf[AkkaPduProtobufCodecConstructMessageMethodInterceptor])
    .advise(method("decodeMessage"), classOf[AkkaPduProtobufCodecDecodeMessageMethodAdvisor])

  /**
    * Instrument:
    *
    * akka.remote.MessageSerializer$::serialize
    * akka.remote.MessageSerializer$::deserialize
    *
    */
  onType("akka.remote.MessageSerializer$")
    .advise(method("serialize"), classOf[MessageSerializerSerializeAdvisor])
    .advise(method("deserialize"), classOf[MessageSerializerDeserializeAdvisor])

}
