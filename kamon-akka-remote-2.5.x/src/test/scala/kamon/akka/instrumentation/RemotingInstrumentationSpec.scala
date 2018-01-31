package kamon.akka.instrumentation

import akka.actor.SupervisorStrategy.Resume
import akka.actor._
import akka.pattern.{ask, pipe}
import akka.remote.RemoteScope
import akka.routing.RoundRobinGroup
import akka.testkit.{ImplicitSender, TestKitBase}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.akka.RemotingMetrics
import kamon.context.Context
import kamon.testkit.{ContextTesting, MetricInspection}
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.Inspectors._
import org.scalatest.Matchers._

import scala.concurrent.duration._
import scala.util.control.NonFatal

class RemotingInstrumentationSpec extends TestKitBase with WordSpecLike with Matchers with ImplicitSender with ContextTesting with MetricInspection {

  implicit lazy val system: ActorSystem = {
    ActorSystem("remoting-spec-local-system", ConfigFactory.parseString(
      """
        |akka {
        |  loggers = ["akka.event.slf4j.Slf4jLogger"]
        |
        |  actor {
        |    provider = "akka.remote.RemoteActorRefProvider"
        |  }
        |  remote {
        |    enabled-transports = ["akka.remote.netty.tcp"]
        |    netty.tcp {
        |      hostname = "127.0.0.1"
        |      port = 2552
        |    }
        |  }
        |}
      """.stripMargin))
  }

  val remoteSystem: ActorSystem = ActorSystem("remoting-spec-remote-system", ConfigFactory.parseString(
    """
      |akka {
      |  loggers = ["akka.event.slf4j.Slf4jLogger"]
      |
      |  actor {
      |    provider = "akka.remote.RemoteActorRefProvider"
      |  }
      |  remote {
      |    enabled-transports = ["akka.remote.netty.tcp"]
      |    netty.tcp {
      |      hostname = "127.0.0.1"
      |      port = 2553
      |    }
      |  }
      |}
    """.stripMargin))

  val RemoteSystemAddress = AddressFromURIString("akka.tcp://remoting-spec-remote-system@127.0.0.1:2553")


  def contextWithBroadcast(name: String): Context =
    Context.create(
      StringBroadcastKey,
      Some(name)
    )

  "The Remoting instrumentation akka-2.5" should {
    "propagate the TraceContext when creating a new remote actor" in {
      Kamon.withContext(contextWithBroadcast("deploy-remote-actor-1")) {
        system.actorOf(TraceTokenReplier.remoteProps(Some(testActor), RemoteSystemAddress), "remote-deploy-fixture")
      }

      expectMsg("name=deploy-remote-actor-1")
    }

    "propagate the TraceContext when sending a message to a remotely deployed actor" in {
      val remoteRef = system.actorOf(TraceTokenReplier.remoteProps(None, RemoteSystemAddress), "remote-message-fixture")

      Kamon.withContext(contextWithBroadcast("message-remote-actor-1")) {
        remoteRef ! "reply-trace-token"
      }
      expectMsg("name=message-remote-actor-1")
    }


    "propagate the TraceContext when pipe or ask a message to a remotely deployed actor" in {
      implicit val ec = system.dispatcher
      implicit val askTimeout = Timeout(10 seconds)
      val remoteRef = system.actorOf(TraceTokenReplier.remoteProps(None, RemoteSystemAddress), "remote-ask-and-pipe-fixture")

      Kamon.withContext(contextWithBroadcast("ask-and-pipe-remote-actor-1")) {
        (remoteRef ? "reply-trace-token") pipeTo testActor
      }

      expectMsg("name=ask-and-pipe-remote-actor-1")
    }
    "propagate the TraceContext when sending a message to an ActorSelection" in {
      remoteSystem.actorOf(TraceTokenReplier.props(None), "actor-selection-target-a")
      remoteSystem.actorOf(TraceTokenReplier.props(None), "actor-selection-target-b")
      val selection = system.actorSelection(RemoteSystemAddress + "/user/actor-selection-target-*")

      Kamon.withContext(contextWithBroadcast("message-remote-actor-selection-1")) {
        selection ! "reply-trace-token"
      }

      // one for each selected actor
      expectMsg("name=message-remote-actor-selection-1")
      expectMsg("name=message-remote-actor-selection-1")
    }




    "propagate the TraceContext when sending messages to remote routees of a router" in {
      remoteSystem.actorOf(TraceTokenReplier.props(None), "router-target-a")
      remoteSystem.actorOf(TraceTokenReplier.props(None), "router-target-b")
      val router = system.actorOf(RoundRobinGroup(List(
        RemoteSystemAddress + "/user/router-target-a",
        RemoteSystemAddress + "/user/router-target-b"
      )).props(), "router")

      Kamon.withContext(contextWithBroadcast("remote-routee-1")) {
        router ! "reply-trace-token"
      }

      expectMsg("name=remote-routee-1")
      expectNoMsg()
    }
    "propagate the TraceContext when a remotely supervised child fails" in {
      val supervisor = system.actorOf(Props(new SupervisorOfRemote(testActor, RemoteSystemAddress)),"SUPERVISOR")

      Kamon.withContext(contextWithBroadcast("remote-supervision-1")) {
        supervisor ! "fail"
      }

      expectMsg(2 minutes,"name=remote-supervision-1")
    }


    "record in/out message counts and sizes for both sending and receiving side" in {
      val outMetricTags = RemotingMetrics.messages.partialRefine(
        Map(
          "system"      -> system.name,
          "direction"   -> "out",
          "peer-system" -> remoteSystem.name
        )
      )
      val inMetricTags = RemotingMetrics.messages.partialRefine(
        Map(
          "system"      -> remoteSystem.name,
          "direction"   -> "in",
          "peer-system" -> system.name
        )
      )
      val (out, in) = (
        RemotingMetrics.messages.refine(outMetricTags.head).distribution(false),
        RemotingMetrics.messages.refine(inMetricTags.head).distribution(false)
      )
      assert(out.max > 0)
      assert(in.max > 0)
      assert(out.count > 0)
      assert(in.count > 0)
    }

    "record de/serialization times for messages" in {
      val systems = Seq(system.name, remoteSystem.name)
      val directions = Seq("out", "in")

      val histograms = for {
        sys <- systems
        dir <- directions
      } yield RemotingMetrics.serialization.refine(
        Map("system" -> sys, "direction" -> dir)
      ).distribution(false)

      val sizes = histograms.map(_.max)

      forAll(sizes) { s => assert(s > 0) }
    }


  }

}

class SupervisorOfRemote(traceContextListener: ActorRef, remoteAddress: Address) extends Actor with ContextTesting {
  val supervisedChild = context.actorOf(TraceTokenReplier.remoteProps(None, remoteAddress), "remotely-supervised-child")

  def receive = {
    case "fail" ⇒  supervisedChild ! "die"
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case NonFatal(throwable) ⇒
      traceContextListener ! currentTraceContextInfo
      Resume
    case _ => Resume
  }

  def currentTraceContextInfo: String = {
    val ctx = Kamon.currentContext()
    val name = ctx.get(StringBroadcastKey).getOrElse("")
    s"name=$name"
  }
}