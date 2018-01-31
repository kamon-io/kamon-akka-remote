package kamon.akka.instrumentation

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.testkit.{ImplicitSender, TestKitBase}
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.context.Context
import kamon.testkit.{ContextTesting, MetricInspection}
import org.scalatest.{Matchers, WordSpecLike}

class ClusterShardingInstrumentationSpec extends TestKitBase with WordSpecLike with Matchers with ImplicitSender with ContextTesting with MetricInspection {

  implicit lazy val system: ActorSystem = {
    ActorSystem("cluster-sharding-spec-system", ConfigFactory.parseString(
      """
        |akka {
        |  loggers = ["akka.event.slf4j.Slf4jLogger"]
        |
        |  actor {
        |    provider = "cluster"
        |  }
        |  remote {
        |    enabled-transports = ["akka.remote.netty.tcp"]
        |    netty.tcp {
        |      hostname = "127.0.0.1"
        |      port = 2554
        |    }
        |  }
        |}
      """.stripMargin))
  }

  val remoteSystem: ActorSystem = ActorSystem("cluster-sharding-spec-remote-system", ConfigFactory.parseString(
    """
      |akka {
      |  loggers = ["akka.event.slf4j.Slf4jLogger"]
      |
      |  actor {
      |    provider = "cluster"
      |  }
      |  remote {
      |    enabled-transports = ["akka.remote.netty.tcp"]
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |      port = 2555
      |    }
      |  }
      |}
    """.stripMargin))

  def contextWithBroadcast(name: String): Context =
    Context.create(
      StringBroadcastKey,
      Some(name)
    )

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case entityId:String ⇒ (entityId, "reply-trace-token")
  }
  val extractShardId: ShardRegion.ExtractShardId = {
    case entityId:String => (entityId.toInt % 10).toString
  }

  "The Cluster-Sharding instrumentation akka-2.5" should {
    "propagate the TraceContext when sending message to sharding region" in {

      Cluster(system).join(Cluster(system).selfAddress)
      Cluster(remoteSystem).join(Cluster(system).selfAddress)

      val replierRegion: ActorRef = ClusterSharding(system).start(
        typeName = "replier",
        entityProps = TraceTokenReplier.props(None),
        settings = ClusterShardingSettings(system),
        extractEntityId = extractEntityId,
        extractShardId = extractShardId)

      Kamon.withContext(contextWithBroadcast("cluster-sharding-actor-123")) {
        replierRegion ! "123"
      }

      expectMsg("name=cluster-sharding-actor-123")
    }
  }

}
