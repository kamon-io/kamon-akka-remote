package akka.kamon.instrumentation.cluster

import java.time.Duration
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.atomic.LongAdder

import akka.actor.ActorRef
import akka.cluster.sharding.Shard
import com.typesafe.config.Config
import kamon.{Kamon, OnReconfigureHook}

import scala.collection.concurrent.TrieMap

object ShardingMetrics {
  val prefix = "akka.cluster.sharding"

  private var samplingSchedule: Option[ScheduledFuture[_]] = None
  private val instruments = TrieMap.empty[String, ShardRegionMonitor]

  private val regionShards = Kamon.rangeSampler(s"$prefix.region.shards")
  private val regionEntities = Kamon.histogram(s"$prefix.region.entities")
  private val regionMessages = Kamon.counter(s"$prefix.region.messages")
  private val shardEntities = Kamon.histogram(s"$prefix.shard.entities")
  private val shardMessages = Kamon.histogram(s"$prefix.shard.messages")

  def byRegion(regionType: String) = ("type" -> regionType)

  def shardsPerRegion(regionType: String) =
    regionShards.refine(byRegion(regionType))

  def entitiesPerRegion(regionType: String) =
    regionEntities.refine(byRegion(regionType))

  def entitiesPerShard(regionType: String) =
    shardEntities.refine(byRegion(regionType))

  def messagesPerRegion(regionType: String) =
    regionMessages.refine(byRegion(regionType))

  def messagesPerShard(regionType: String) =
    shardMessages.refine(byRegion(regionType))

  def forType(shardedType: String): ShardRegionMonitor =
    instruments.getOrElseUpdate(shardedType, {
      startSampling()
      new ShardRegionMonitor(shardedType)
    })

  def cleanInstrumentation(shardedType: String): Unit = {
    instruments.get(shardedType).foreach(_.sample())
    instruments.remove(shardedType)
    if(instruments.isEmpty) {
      stopSampling()
    }
  }

  private def startSampling(): Unit = {
    if (samplingSchedule.isEmpty) {
      this.samplingSchedule = Some(
        Kamon
          .scheduler()
          .scheduleAtFixedRate(
            new Runnable {
              override def run(): Unit = instruments.values.foreach(_.sample())
            },
            samplingPeriod.toNanos,
            samplingPeriod.toNanos,
            java.util.concurrent.TimeUnit.NANOSECONDS
          )
      )
    }
  }

  private def stopSampling(): Unit = {
    this.samplingSchedule.foreach(_.cancel(false))
    this.samplingSchedule = None
  }

  private def getConfiguredSamplingPeriod(config: Config): Duration =
    config.getDuration("kamon.akka.cluster.sharding.sampling-period")

  @volatile private var samplingPeriod: Duration = getConfiguredSamplingPeriod(Kamon.config())

  Kamon.onReconfigure(new OnReconfigureHook {
    override def onReconfigure(newConfig: Config): Unit = {
      samplingPeriod = getConfiguredSamplingPeriod(newConfig)
      stopSampling()
      startSampling()
    }
  })

}

class ShardRegionMonitor(shardedType: String) {
  import ShardingMetrics._

  private val shards = TrieMap.empty[ActorRef, (Shard, LongAdder)]

  def shardMessage(shardRef: ActorRef) = {
    shards.get(shardRef).foreach { case (_, counter) => counter.increment() }
  }

  def regionMessage(): Unit =
    messagesPerRegion(shardedType).increment()

  def shardStarted(shardRef: ActorRef, shard: Shard) = {
    shardsPerRegion(shardedType).increment
    shards.put(shardRef, (shard, new LongAdder))
  }

  def shardStopped(shardRef: ActorRef): Unit = {
    shards.remove(shardRef)
    shardsPerRegion(shardedType).decrement()
  }

  def sample(): Unit = {
    shards.values.foreach {
      case (shard, counter) =>
        messagesPerShard(shardedType).record(counter.sumThenReset())
        entitiesPerShard(shardedType).record(shard.state.entities.size)
    }

    entitiesPerRegion(shardedType).record {
      shards.values.foldLeft(0L)((acc, pair) => {
        val (shard, _) = pair
        acc + shard.state.entities.size
      })
    }
  }
}
