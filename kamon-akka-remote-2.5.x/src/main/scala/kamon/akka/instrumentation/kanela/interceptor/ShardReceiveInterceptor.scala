package akka.kamon.instrumentation.kanela.interceptor

import java.util.concurrent.Callable

import akka.actor.Actor
import akka.cluster.sharding.Shard
import akka.cluster.sharding.ShardCoordinator.Internal.HandOff
import akka.kamon.instrumentation.cluster.{ShardedType, ShardingMetrics}
import kanela.agent.libs.net.bytebuddy.implementation.bind.annotation._

object ShardReceiveInterceptor {
  @RuntimeType
  def aroundReceive(@SuperCall callable: Callable[_], @This shard: ShardedType): AnyRef = {
    val tpe = shard.typeName
    val metrics = ShardingMetrics.forType(tpe)

    def handle(msg: Any) = msg match {
      case msg if shard.idExtractor.isDefinedAt(msg) =>
        metrics.shardMessage(shard.asInstanceOf[Shard].context.self)
      case HandOff(_) =>
        metrics.shardStopped(shard.asInstanceOf[Shard].context.self)
      case _ => ()
    }

    val receive: PartialFunction[Any, Unit] = callable.call().asInstanceOf[Actor.Receive]
    val wrappedReceive: Actor.Receive = {
      case msg: Any =>
        handle(msg)
        receive.apply(msg)
    }

    wrappedReceive
  }
}
