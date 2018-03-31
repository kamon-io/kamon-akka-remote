package akka.kamon.instrumentation.cluster

import akka.actor.{ActorRef, Terminated}
import akka.cluster.Cluster
import akka.cluster.sharding.ShardCoordinator.Internal.HandOff
import akka.cluster.sharding.{Shard, ShardRegion}
import akka.cluster.sharding.ShardRegion.{GracefulShutdown, _}
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

import scala.concurrent.duration._
@Aspect
class ShardingInstrumentation {

  @Pointcut(
    "execution(akka.cluster.sharding.ShardRegion.new(..)) && this(region) && args(typeName, *, *, *, *, extractEntityId, ..)")
  def regionCreate(region: ShardRegion,
                   typeName: String,
                   extractEntityId: ShardRegion.ExtractEntityId): Unit = {}

  @After("regionCreate(region, typeName, extractEntityId)")
  def afterRegionCreate(region: ShardRegion,
                        typeName: String,
                        extractEntityId: ShardRegion.ExtractEntityId): Any = {
    val withIdentity = region.asInstanceOf[ShardedType]
    withIdentity.setTypeName(typeName)
    withIdentity.setIdExtractor(extractEntityId)
  }

  @Pointcut(
    "execution(* akka.cluster.sharding.ShardRegion.receive()) && this(region)")
  def msgReceive(region: ShardRegion): Unit = {}

  @Around("msgReceive(region)")
  def aroungMsgReceive(pjp: ProceedingJoinPoint, region: ShardRegion): PartialFunction[Any, Unit] = {
    val identifiedRegion = region.asInstanceOf[ShardedType]
    val metrics = ShardingMetrics.forType(identifiedRegion.typeName)

    def handle(msg: Any) = msg match {
      case msg if identifiedRegion.idExtractor.isDefinedAt(msg) =>
        metrics.regionMessage
      case _ => ()
    }

    val receive: PartialFunction[Any, Unit] =
      pjp.proceed().asInstanceOf[PartialFunction[Any, Unit]]
    val wrappedReceive: PartialFunction[Any, Unit] = {
      case msg: Any =>
        handle(msg)
        receive.apply(msg)
    }
    wrappedReceive
  }

  @Pointcut(
    "execution(* akka.cluster.sharding.ShardRegion.postStop()) && this(region)")
  def regionStop(region: ShardRegion): Unit = {}

  @After("regionStop(region)")
  def afterRegionStop(region: ShardRegion): Any = {
    ShardingMetrics.cleanInstrumentation(
      region.asInstanceOf[ShardedType].typeName)
  }

  @Pointcut(
    "execution(akka.cluster.sharding.Shard.new(..)) && this(shard) && args(typeName, *, *, *, extractEntityId, ..)")
  def shardCreate(shard: Shard,
                  typeName: String,
                  extractEntityId: ExtractEntityId): Unit = {}

  @After("shardCreate(shard, typeName, extractEntityId)")
  def aroundShardCreate(shard: Shard,
                        typeName: String,
                        extractEntityId: ExtractEntityId): Any = {
    val withIdentity = shard.asInstanceOf[ShardedType]
    withIdentity.setTypeName(typeName)
    withIdentity.setIdExtractor(extractEntityId)
    ShardingMetrics.forType(typeName).shardStarted(shard.context.self, shard)
  }

  @Pointcut("execution(* akka.cluster.sharding.Shard.receive()) && this(shard)")
  def shardReceive(shard: Shard): Unit = {}

  @Around("shardReceive(shard)")
  def aroundShardreceive(pjp: ProceedingJoinPoint, shard: Shard): PartialFunction[Any, Unit] = {
    val identifiedShard = shard.asInstanceOf[ShardedType]
    val tpe = identifiedShard.typeName
    val metrics = ShardingMetrics.forType(tpe)

    def handle(msg: Any) = msg match {
      case msg if identifiedShard.idExtractor.isDefinedAt(msg) =>
        metrics.shardMessage(shard.context.self)
      case HandOff(_) =>
        metrics.shardStopped(shard.context.self)
      case _ => ()
    }

    val receive: PartialFunction[Any, Unit] =
      pjp.proceed().asInstanceOf[PartialFunction[Any, Unit]]
    val wrappedReceive: PartialFunction[Any, Unit] = {
      case msg: Any =>
        handle(msg)
        receive.apply(msg)
    }
    wrappedReceive
  }
}

trait ShardedType {
  def typeName: String
  def setTypeName(typeName: String)

  def idExtractor: ShardRegion.ExtractEntityId
  def setIdExtractor(extractor: ShardRegion.ExtractEntityId): Unit
}

class InjectedShardedType extends ShardedType {
  var typeName: String = _
  var idExtractor: ShardRegion.ExtractEntityId = _

  override def setTypeName(identity: String): Unit = this.typeName = identity
  override def setIdExtractor(extractor: ExtractEntityId): Unit =
    this.idExtractor = extractor
}

@Aspect
class IdentifiableElements {
  @DeclareMixin("akka.cluster.sharding.ShardRegion")
  def identityIntoShardRegion: ShardedType = new InjectedShardedType
  @DeclareMixin("akka.cluster.sharding.Shard")
  def intoShard: ShardedType = new InjectedShardedType
}
