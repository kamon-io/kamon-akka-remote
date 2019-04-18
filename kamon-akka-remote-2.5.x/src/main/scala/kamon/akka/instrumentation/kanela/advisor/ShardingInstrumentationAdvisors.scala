package akka.kamon.instrumentation.kanela.advisor

import _root_.kanela.agent.libs.net.bytebuddy.asm.Advice._
import akka.cluster.sharding.{Shard, ShardRegion}
import akka.kamon.instrumentation.cluster.{ShardedType, ShardingMetrics}
import kamon.akka.Akka
import kamon.akka.instrumentation.kanela.cluster.ShardingInstrumentation.regionGroupName
import kamon.util.Filter.Glob

class ShardRegionConstructorAdvisor
object ShardRegionConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This region: ShardRegion,
             @Argument(0) typeName: String,
             @Argument(5) extractEntityId: ShardRegion.ExtractEntityId): Unit = {
    val withIdentity = region.asInstanceOf[ShardedType]
    withIdentity.setTypeName(typeName)
    withIdentity.setIdExtractor(extractEntityId)

    val system = region.context.system
    val shardingGuardian = system.settings.config.getString("akka.cluster.sharding.guardian-name")
    val entitiesPath = s"${system.name}/system/$shardingGuardian/$typeName/*/*"
    Akka.addActorGroup(regionGroupName(typeName), Glob(entitiesPath))
  }

}

class ShardConstructorAdvisor
object ShardConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This shard: Shard,
             @Argument(0) typeName: String,
             @Argument(4) extractEntityId: ShardRegion.ExtractEntityId): Unit = {
    val withIdentity = shard.asInstanceOf[ShardedType]
    withIdentity.setTypeName(typeName)
    withIdentity.setIdExtractor(extractEntityId)
    ShardingMetrics.forType(typeName).shardStarted(shard.context.self, shard)
  }

}

class ShardRegionPostStopAdvisor
object ShardRegionPostStopAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This region: ShardRegion): Unit = {
    ShardingMetrics.cleanInstrumentation(
      region.asInstanceOf[ShardedType].typeName)
  }
}


