package kamon.akka.instrumentation.kanela.cluster

import akka.kamon.instrumentation.cluster.InjectedShardedType
import akka.kamon.instrumentation.kanela.advisor.{ShardConstructorAdvisor, ShardRegionConstructorAdvisor, ShardRegionPostStopAdvisor}
import akka.kamon.instrumentation.kanela.interceptor.{ShardReceiveInterceptor, ShardRegionReceiveInterceptor}
import kamon.akka.instrumentation.kanela.mixin.InjectedShardedTypeMixin
import kanela.agent.scala.KanelaInstrumentation

class ShardingInstrumentation extends KanelaInstrumentation {

  forTargetType("akka.cluster.sharding.ShardRegion") { builder ⇒
    builder
        .withMixin(classOf[InjectedShardedTypeMixin])
        .withAdvisorFor(Constructor, classOf[ShardRegionConstructorAdvisor])
        .withInterceptorFor(method("receive"), ShardRegionReceiveInterceptor)
        .withAdvisorFor(method("postStop"), classOf[ShardRegionPostStopAdvisor])
        .build()
  }

  forTargetType("akka.cluster.sharding.Shard") { builder ⇒
    builder
      .withMixin(classOf[InjectedShardedTypeMixin])
      .withAdvisorFor(Constructor, classOf[ShardConstructorAdvisor])
      .withInterceptorFor(method("receive"), ShardReceiveInterceptor)
      .build()
  }

}

object ShardingInstrumentation {

  def regionGroupName(regionTypeName: String): String =
    s"shardRegion/$regionTypeName"

}