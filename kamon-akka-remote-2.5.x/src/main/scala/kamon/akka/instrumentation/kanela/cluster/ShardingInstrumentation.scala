package kamon.akka.instrumentation.kanela.cluster

import akka.kamon.instrumentation.kanela.advisor.{ShardConstructorAdvisor, ShardRegionConstructorAdvisor, ShardRegionPostStopAdvisor}
import akka.kamon.instrumentation.kanela.interceptor.{ShardReceiveInterceptor, ShardRegionReceiveInterceptor}
import kamon.akka.instrumentation.kanela.AkkaVersionedFilter
import kamon.akka.instrumentation.kanela.mixin.InjectedShardedTypeMixin
import kanela.agent.scala.KanelaInstrumentation

class ShardingInstrumentation extends KanelaInstrumentation with AkkaVersionedFilter {

  /**
    * Instrument:
    *
    * akka.cluster.sharding.ShardRegion::constructor
    * akka.cluster.sharding.ShardRegion::receive
    * akka.cluster.sharding.ShardRegion::postStop
    *
    * Mix:
    *
    * akka.cluster.sharding.ShardRegion with kamon.akka.instrumentation.kanela.mixin.InjectedShardedTypeMixin
    *
    */
  forTargetType("akka.cluster.sharding.ShardRegion") { builder ⇒
    filterAkkaVersion(builder)
        .withMixin(classOf[InjectedShardedTypeMixin])
        .withAdvisorFor(Constructor, classOf[ShardRegionConstructorAdvisor])
        .withInterceptorFor(method("receive"), ShardRegionReceiveInterceptor)
        .withAdvisorFor(method("postStop"), classOf[ShardRegionPostStopAdvisor])
        .build()
  }

  /**
    * Instrument:
    *
    * akka.cluster.sharding.Shard::constructor
    * akka.cluster.sharding.Shard::receive
    *
    * Mix:
    *
    * akka.cluster.sharding.Shard with kamon.akka.instrumentation.kanela.mixin.InjectedShardedTypeMixin
    *
    */
  forTargetType("akka.cluster.sharding.Shard") { builder ⇒
    filterAkkaVersion(builder)
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