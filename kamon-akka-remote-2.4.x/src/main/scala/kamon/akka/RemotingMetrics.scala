package kamon.akka

import akka.actor.Address
import kamon.Kamon

object RemotingMetrics {
  val messages = Kamon.histogram("akka.remote.message-bytes")
  val serialization = Kamon.histogram("akka.remote.serialization-time")


  def recordMessageInbound(localAddress: Address, senderAddress: Option[Address], size: Long): Unit = recordMessage(localAddress, senderAddress, size, "in")

  def recordOutboundMessage(localAddress: Address, recipientAddress: Option[Address], size: Long): Unit = recordMessage(localAddress, recipientAddress, size, "out")

  private def recordMessage(localAddress: Address, peerAddress: Option[Address], size: Long, direction: String): Unit = {
    val localHost = for {
      host <- localAddress.host
      port <- localAddress.port
    } yield s"$host:$port"

    val peerHost = for {
      addr <- peerAddress
      host <- addr.host
      port <- addr.port
    } yield s"$host:$port"

    messages.refine(
      Map(
        "system"      -> localAddress.system,
        "host"        -> localHost.getOrElse(""),
        "direction"   -> direction,
        "peer-system" -> peerAddress.map(_.system).getOrElse(""),
        "peer-host"   -> peerHost.getOrElse("")
      )
    ).record(size)
  }

  def recordSerialization(system: String, time: Long) = serialization.refine(Map(
    "system" -> system,
    "direction" -> "serialization"
  )).record(time)

  def recordDeserialization(system: String, time: Long) = serialization.refine(Map(
    "system" -> system,
    "direction" -> "deserialization"
  )).record(time)



}
