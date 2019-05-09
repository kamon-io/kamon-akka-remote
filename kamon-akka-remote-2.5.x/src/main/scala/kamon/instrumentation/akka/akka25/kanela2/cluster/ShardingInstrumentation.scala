package kamon.instrumentation.akka.akka25.kanela2.cluster

import akka.kamon.instrumentation.kanela.advisor.{ShardConstructorAdvisor, ShardRegionConstructorAdvisor, ShardRegionPostStopAdvisor}
import akka.kamon.instrumentation.kanela.interceptor.{ShardReceiveInterceptor, ShardRegionReceiveInterceptor}
import kamon.instrumentation.akka.akka25.kanela2.mixin.InjectedShardedTypeMixin
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
    * akka.cluster.sharding.ShardRegion with kamon.instrumentation.akka.akka25.kanela.mixin.InjectedShardedTypeMixin
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
    * akka.cluster.sharding.Shard with kamon.instrumentation.akka.akka25.kanela.mixin.InjectedShardedTypeMixin
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