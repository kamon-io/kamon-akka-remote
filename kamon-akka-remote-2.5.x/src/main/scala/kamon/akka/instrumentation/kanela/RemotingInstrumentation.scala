package kamon.akka.instrumentation.kanela

import akka.kamon.instrumentation.kanela.interceptor.AkkaPduProtobufCodecConstructMessageMethodInterceptor
import akka.remote.kamon.instrumentation.kanela.advisor.{EndpointWriterWriteSendMethodAdvisor, SendConstructorAdvisor, SendSystemMessageMethodAdvisor}
import kamon.akka.instrumentation.kanela.mixin.HasTransientContextMixin
import kanela.agent.scala.KanelaInstrumentation

class RemotingInstrumentation extends KanelaInstrumentation {

  forTargetType("akka.remote.EndpointManager.Send") { builder ⇒
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

//  forTargetType("akka.actor.ActorCell") { builder ⇒
//    builder
//      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
//      .build()
//  }
//
//  forTargetType("akka.actor.UnstartedCell") { builder ⇒
//    builder
//      .withAdvisorFor(method("sendSystemMessage"), classOf[SendSystemMessageMethodAdvisor])
//      .build()
//  }

  forTargetType("akka.remote.transport.AkkaPduProtobufCodec$") { builder ⇒
    builder
      .withInterceptorFor(method("constructMessage"), AkkaPduProtobufCodecConstructMessageMethodInterceptor)
      .build()
  }


}
