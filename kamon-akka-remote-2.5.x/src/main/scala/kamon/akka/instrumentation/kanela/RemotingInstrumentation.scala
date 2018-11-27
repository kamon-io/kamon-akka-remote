package kamon.akka.instrumentation.kanela

import akka.kamon.akka.instrumentation.kanela.ReplaceWithMethodInterceptor
import akka.kamon.instrumentation.kanela.advisor._
import akka.kamon.instrumentation.kanela.interceptor.{AkkaPduProtobufCodecConstructMessageMethodInterceptor, InvokeAllMethodInterceptor}
import akka.remote.kamon.instrumentation.kanela.advisor._
import kamon.Kamon
import kamon.akka.instrumentation.kanela.mixin.{ActorInstrumentationMixin, HasTransientContextMixin}
import kanela.agent.scala.KanelaInstrumentation

class RemotingInstrumentation extends KanelaInstrumentation with AkkaVersionedFilter {
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
  forTargetType("akka.remote.EndpointManager$Send") { builder ⇒
    filterAkkaVersion(builder)
      .withMixin(classOf[HasTransientContextMixin])
      .withAdvisorFor(Constructor, classOf[SendConstructorAdvisor])
      .build()
  }

  /**
    * Instrument:
    *
    * akka.remote.EndpointWriter::writeSend
    *
    */
  forTargetType("akka.remote.EndpointWriter") { builder ⇒
    filterAkkaVersion(builder)
      .withAdvisorFor(method("writeSend"), classOf[EndpointWriterWriteSendMethodAdvisor])
      .build()
  }

  /**
    * Instrument:
    *
    * akka.actor.ActorCell::sendSystemMessage
    *
    */
  forTargetType("akka.actor.ActorCell") { builder ⇒
    filterAkkaVersion(builder)
      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
      .build()
  }

  /**
    * Instrument:
    *
    * akka.actor.UnstartedCell::sendSystemMessage
    *
    */
  forTargetType("akka.actor.UnstartedCell") { builder ⇒
    filterAkkaVersion(builder)
      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
      .build()
  }

  /**
    * Instrument:
    *
    * akka.remote.transport.AkkaPduProtobufCodec$::constructMessage
    * akka.remote.transport.AkkaPduProtobufCodec$::decodeMessage
    *
    */
  forTargetType("akka.remote.transport.AkkaPduProtobufCodec$") { builder ⇒
    filterAkkaVersion(builder)
      .withInterceptorFor(method("constructMessage"), AkkaPduProtobufCodecConstructMessageMethodInterceptor)
      .withAdvisorFor(method("decodeMessage"), classOf[AkkaPduProtobufCodecDecodeMessageMethodAdvisor])
      .build()
  }

  /**
    * Instrument:
    *
    * akka.remote.MessageSerializer$::serialize
    * akka.remote.MessageSerializer$::deserialize
    *
    */
  forTargetType("akka.remote.MessageSerializer$") { builder =>
    filterAkkaVersion(builder)
      .withAdvisorFor(method("serialize"), classOf[MessageSerializerSerializeAdvisor])
      .withAdvisorFor(method("deserialize"), classOf[MessageSerializerDeserializeAdvisor])
      .build()
  }

}
