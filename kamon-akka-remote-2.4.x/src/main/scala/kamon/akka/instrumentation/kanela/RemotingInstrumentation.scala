package kamon.akka.instrumentation.kanela

import akka.kamon.instrumentation.kanela.advisor._
import akka.kamon.instrumentation.kanela.interceptor.{AkkaPduProtobufCodecConstructMessageMethodInterceptor, InvokeAllMethodInterceptor}
import akka.remote.kamon.instrumentation.kanela.advisor._
import kamon.Kamon
import kamon.akka.instrumentation.kanela.mixin.{ActorInstrumentationMixin, HasTransientContextMixin}
import kanela.agent.scala.KanelaInstrumentation

class RemotingInstrumentation extends KanelaInstrumentation with AkkaVersionedFilter {

  forTargetType("akka.remote.EndpointManager$Send") { builder ⇒
    filterAkkaVersion(builder)
      .withMixin(classOf[HasTransientContextMixin])
      .withAdvisorFor(Constructor, classOf[SendConstructorAdvisor])
      .build()
  }

  forTargetType("akka.remote.EndpointWriter") { builder ⇒
    filterAkkaVersion(builder)
      .withAdvisorFor(method("writeSend"), classOf[EndpointWriterWriteSendMethodAdvisor])
      .build()
  }

  forTargetType("akka.actor.ActorCell") { builder ⇒
    filterAkkaVersion(builder)
      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
      .build()
  }

  forTargetType("akka.actor.UnstartedCell") { builder ⇒
    filterAkkaVersion(builder)
      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
      .build()
  }

  forTargetType("akka.remote.transport.AkkaPduProtobufCodec$") { builder ⇒
    filterAkkaVersion(builder)
      .withInterceptorFor(method("constructMessage"), AkkaPduProtobufCodecConstructMessageMethodInterceptor)
      .withAdvisorFor(method("decodeMessage"), classOf[AkkaPduProtobufCodecDecodeMessageMethodAdvisor])
      .build()
  }

  forTargetType("akka.remote.MessageSerializer$") { builder =>
    filterAkkaVersion(builder)
      .withAdvisorFor(method("serialize"), classOf[MessageSerializerSerializeAdvisor])
      .withAdvisorFor(method("deserialize"), classOf[MessageSerializerDeserializeAdvisor])
      .build()
  }

}
