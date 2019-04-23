package kamon.akka.instrumentation.kanela.cluster

import akka.kamon.instrumentation.kanela.advisor.{ShardConstructorAdvisor, ShardRegionConstructorAdvisor, ShardRegionPostStopAdvisor}
import akka.kamon.instrumentation.kanela.interceptor.{ShardReceiveInterceptor, ShardRegionReceiveInterceptor}
import kamon.akka.instrumentation.kanela.mixin.InjectedShardedTypeMixin
import kanela.agent.api.instrumentation.InstrumentationBuilder

class ShardingInstrumentation extends InstrumentationBuilder {

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
  onType("akka.cluster.sharding.ShardRegion")
    .mixin(classOf[InjectedShardedTypeMixin])
    .advise(isConstructor, classOf[ShardRegionConstructorAdvisor])
    .intercept(method("receive"), new ShardRegionReceiveInterceptor)
    .advise(method("postStop"), classOf[ShardRegionPostStopAdvisor])

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
  onType("akka.cluster.sharding.Shard")
    .mixin(classOf[InjectedShardedTypeMixin])
    .advise(isConstructor, classOf[ShardConstructorAdvisor])
    .intercept(method("receive"), new ShardReceiveInterceptor)

}

object ShardingInstrumentation {

  def regionGroupName(regionTypeName: String): String =
    s"shardRegion/$regionTypeName"

}