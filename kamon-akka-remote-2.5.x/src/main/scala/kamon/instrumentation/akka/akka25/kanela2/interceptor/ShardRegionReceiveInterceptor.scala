package akka.kamon.instrumentation.kanela.interceptor

import java.util.concurrent.Callable

import akka.actor.Actor
import akka.cluster.sharding.ShardRegion
import akka.kamon.instrumentation.cluster.ShardingMetrics
import kamon.instrumentation.akka.akka25.ShardedType
import kanela.agent.libs.net.bytebuddy.implementation.bind.annotation._

class ShardRegionReceiveInterceptor {

  @RuntimeType
  def aroundReceive(@SuperCall callable: Callable[_], @This region: ShardedType): AnyRef = {
    val identifiedRegion = region.asInstanceOf[ShardedType]
    val metrics = ShardingMetrics.forType(identifiedRegion.typeName)

    def handle(msg: Any) = msg match {
      case msg if identifiedRegion.idExtractor.isDefinedAt(msg) =>
        metrics.regionMessage
      case _ => ()
    }

    val receive: Actor.Receive = callable.call().asInstanceOf[Actor.Receive]
    val wrappedReceive: Actor.Receive = {
      case msg: Any =>
        handle(msg)
        receive.apply(msg)
    }

    wrappedReceive
  }

}
