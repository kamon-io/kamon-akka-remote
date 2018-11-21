package kamon.akka.instrumentation.kanela

import akka.kamon.akka.instrumentation.kanela.ReplaceWithMethodInterceptor
import akka.kamon.instrumentation.kanela.advisor._
import akka.kamon.instrumentation.kanela.interceptor.{AkkaPduProtobufCodecConstructMessageMethodInterceptor, InvokeAllMethodInterceptor}
import akka.remote.kamon.instrumentation.kanela.advisor._
import kamon.Kamon
import kamon.akka.instrumentation.kanela.mixin.{ActorInstrumentationMixin, HasTransientContextMixin}
import kanela.agent.scala.KanelaInstrumentation

class RemotingInstrumentation extends KanelaInstrumentation {

  forTargetType("akka.remote.EndpointManager$Send") { builder ⇒
    builder
      .withMixin(classOf[HasTransientContextMixin])
      .withAdvisorFor(Constructor, classOf[SendConstructorAdvisor])
      .build()
  }

  forTargetType("akka.remote.EndpointWriter") { builder ⇒
    builder
      .withAdvisorFor(method("writeSend"), classOf[EndpointWriterWriteSendMethodAdvisor])
      .build()
  }

  forTargetType("akka.actor.ActorCell") { builder ⇒
    builder
      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
      .build()
  }

  forTargetType("akka.actor.UnstartedCell") { builder ⇒
    builder
      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
      .build()
  }

  forTargetType("akka.remote.transport.AkkaPduProtobufCodec$") { builder ⇒
    builder
      .withInterceptorFor(method("constructMessage"), AkkaPduProtobufCodecConstructMessageMethodInterceptor)
      .withAdvisorFor(method("decodeMessage"), classOf[AkkaPduProtobufCodecDecodeMessageMethodAdvisor])
      .build()
  }

  forTargetType("akka.remote.MessageSerializer$") { builder =>
    builder
      .withAdvisorFor(method("serialize"), classOf[MessageSerializerSerializeAdvisor])
      .withAdvisorFor(method("deserialize"), classOf[MessageSerializerDeserializeAdvisor])
      .build()
  }

}
