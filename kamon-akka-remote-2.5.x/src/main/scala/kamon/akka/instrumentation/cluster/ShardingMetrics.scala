package akka.kamon.instrumentation.cluster

import java.time.Duration
import java.util.concurrent.{ConcurrentHashMap, ScheduledFuture}
import java.util.concurrent.atomic.LongAdder

import akka.actor.ActorRef
import akka.cluster.sharding.Shard
import com.typesafe.config.Config
import kamon.{Kamon, OnReconfigureHook}

import scala.collection.mutable
import scala.collection.JavaConverters._

object ShardingMetrics {

  val prefix = "akka.cluster.sharding"

  private var samplingSchedule: Option[ScheduledFuture[_]] = None

  private val instruments: mutable.Map[String, RegionInstrumentation] =
    new ConcurrentHashMap[String, RegionInstrumentation]().asScala

  private val regionShards = Kamon.gauge(s"$prefix.region.shards")
  private val regionEntities = Kamon.gauge(s"$prefix.region.entities")
  private val regionMessages = Kamon.counter(s"$prefix.region.messages")

  private val shardEntities = Kamon.histogram(s"$prefix.shards.entities")
  private val shardMessages = Kamon.histogram(s"$prefix.shards.messages")

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

  def forType(shardedType: String): RegionInstrumentation =
    instruments.getOrElseUpdate(shardedType, {
      startSampling
      new RegionInstrumentation(shardedType)
    })

  def cleanInstrumentation(shardedType: String): Unit = {
    instruments.get(shardedType).foreach(_.sample)
    instruments.remove(shardedType)
    if (instruments.isEmpty) stopSampling
  }

  private def startSampling = {
    if (samplingSchedule.isEmpty) {
      this.samplingSchedule = Some(
        Kamon
          .scheduler()
          .scheduleAtFixedRate(
            new Runnable {
              override def run(): Unit = instruments.values.foreach(_.sample)
            },
            samplingPeriod.toNanos,
            samplingPeriod.toNanos,
            java.util.concurrent.TimeUnit.NANOSECONDS
          )
      )
    }
  }

  private def stopSampling = {
    this.samplingSchedule.foreach(_.cancel(false))
    this.samplingSchedule = None
  }

  private def getConfiguredSamplingPeriod(config: Config): Duration =
    config.getDuration("kamon.akka.cluster.sharding.sampling-period")

  private var samplingPeriod: Duration = getConfiguredSamplingPeriod(
    Kamon.config())
}

class RegionInstrumentation(shardedType: String) {
  import ShardingMetrics._

  private val shards: mutable.Map[ActorRef, (Shard, LongAdder)] =
    new ConcurrentHashMap[ActorRef, (Shard, LongAdder)]().asScala

  def shardMessage(shardRef: ActorRef) = {
    shards.get(shardRef).foreach { case (_, counter) => counter.increment() }
  }

  def regionMessage = messagesPerRegion(shardedType).increment()

  def shardStarted(shardRef: ActorRef, shard: Shard) = {
    shardsPerRegion(shardedType).increment
    shards.put(shardRef, (shard, new LongAdder))
  }

  def shardStopped(shardRef: ActorRef) = {
    shards.remove(shardRef)
    shardsPerRegion(shardedType).decrement()
  }

  def sample = {
    shards.values.foreach {
      case (shard, counter) =>
        messagesPerShard(shardedType).record(counter.sumThenReset())
        entitiesPerShard(shardedType).record(shard.state.entities.size)
    }
    entitiesPerRegion(shardedType).set(
      shards.values.map(_._1.state.entities.size).sum)
  }

}
