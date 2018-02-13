package akka.instrumentation.test

import akka.actor._
import akka.cluster.sharding.ShardCoordinator.Internal.{HandOff, ShardStopped}
import akka.cluster.sharding.ShardCoordinator.ShardAllocationStrategy
import akka.cluster.sharding.ShardRegion.{GracefulShutdown, ShardId}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.kamon.instrumentation.cluster.ShardingMetrics
import akka.testkit.TestActor.Watch
import akka.testkit.{ImplicitSender, TestKitBase}
import com.typesafe.config.ConfigFactory
import kamon.testkit.MetricInspection
import org.scalatest.{Matchers, WordSpecLike}

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.Random
case class TestMessage(shard: String, entity: String)

class ShardingInstrumentationSpec
    extends TestKitBase
    with WordSpecLike
    with Matchers
    with ImplicitSender
    with MetricInspection {
  import ShardingMetrics._

  lazy val system: ActorSystem = {
    ActorSystem(
      "sharding",
      ConfigFactory
        .parseString("""
        |akka {
        |  actor.provider = "cluster"
        |  remote {
        |    enabled-transports = ["akka.remote.netty.tcp"]
        |    netty.tcp {
        |      hostname = "127.0.0.1"
        |      port = 2551
        |    }
        |  }
        |  loglevel = "DEBUG"
        |  cluster {
        |    seed-nodes = ["akka.tcp://sharding@127.0.0.1:2551"]
        |    log-info = on
        |    cluster.jmx.multi-mbeans-in-same-jvm = on
        |  }
        |}
      """.stripMargin)
        .withFallback(ConfigFactory.load())
    )
  }

  val entityIdExtractor: ShardRegion.ExtractEntityId = {
    case msg @ TestMessage(_, entity) => (entity, msg)
  }

  def shardIdExtractor: ShardRegion.ExtractShardId = {
    case msg @ TestMessage(shard, _) => shard
  }

  val StaticAllocationStrategy = new ShardAllocationStrategy {
    override def allocateShard(
        requester: ActorRef,
        shardId: ShardId,
        currentShardAllocations: Map[ActorRef, immutable.IndexedSeq[ShardId]])
      : Future[ActorRef] = {
      Future.successful(requester)
    }
    override def rebalance(
        currentShardAllocations: Map[ActorRef, immutable.IndexedSeq[ShardId]],
        rebalanceInProgress: Set[ShardId]): Future[Set[ShardId]] = {
      Future.successful(Set.empty)
    }
  }

  def registerTypes(shardedType: String,
                    props: Props,
                    system: ActorSystem,
                    allocationStrategy: ShardAllocationStrategy) =

    ClusterSharding(system).start(
      typeName = shardedType,
      entityProps = props,
      settings = ClusterShardingSettings(system),
      extractEntityId = entityIdExtractor,
      extractShardId = shardIdExtractor,
      allocationStrategy = allocationStrategy,
      handOffStopMessage = PoisonPill
    )

  class ShardedTypeContext  {
    val tpe = s"TestType-${Random.nextLong()}"
    val region = registerTypes(tpe, TestActor.props(testActor), system, StaticAllocationStrategy)
  }

  "Cluster sharding instrumentation" should {
    "track shards, entities and messages" in new ShardedTypeContext {
      region ! TestMessage("s1", "e1")
      region ! TestMessage("s1", "e2")
      region ! TestMessage("s2", "e3")

      (1 to 3).foreach(_ => expectMsg("OK"))

      shardsPerRegion(tpe).value(true) should be(2)
      entitiesPerRegion(tpe).value(true) should be(3)

      val shardentityDistribution = entitiesPerShard(tpe).distribution(true)
      shardentityDistribution.max should be(2)

      messagesPerRegion(tpe).value(true) should be(3)
      messagesPerShard(tpe).distribution(true).sum should be(3)
    }

    "clean metrics on handoff" in new ShardedTypeContext {
      region ! TestMessage("s1", "e1")
      expectMsg("OK")

      shardsPerRegion(tpe).value(false) should be(1)
      entitiesPerRegion(tpe).value(false) should be(1)
      entitiesPerShard(tpe).distribution(false).max should be(1)

      region ! HandOff("s1")
      expectMsg(ShardStopped("s1"))

      shardsPerRegion(tpe).value(true) should be(0)
      entitiesPerRegion(tpe).value(true) should be(0)
    }

    "clean metrics on shutdown" in new ShardedTypeContext {
      region ! TestMessage("s1", "e1")
      expectMsg("OK")

      shardsPerRegion(tpe).value(false) should be(1)
      entitiesPerShard(tpe).distribution(false).max should be(1)
      entitiesPerRegion(tpe).value(false) should be(1)

      testActor ! Watch(region)

      region ! GracefulShutdown

      expectTerminated(region)

      shardsPerRegion(tpe).value(false) should be(0)
      entitiesPerRegion(tpe).value(false) should be(0)
    }

  }

}

object TestActor {
  val shardedType = "Test"
  def props(testActor: ActorRef) = Props(classOf[TestActor], testActor)
}

class TestActor(testActor: ActorRef) extends Actor {

  override def receive = {
    case _ => testActor ! "OK"
  }
}
