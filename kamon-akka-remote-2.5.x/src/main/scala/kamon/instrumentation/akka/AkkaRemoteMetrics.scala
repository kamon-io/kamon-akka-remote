package kamon.instrumentation.akka

import akka.actor.Address
import kamon.Kamon
import kamon.metric.InstrumentGroup
import kamon.metric.MeasurementUnit.information
import kamon.tag.TagSet

object AkkaRemoteMetrics {

  val MessageSize = Kamon.histogram (
    name = "akka.remote.message-size",
    description = "Tracks the distribution of incoming and outgoing message sizes",
    unit = information.bytes
  )

  val SerializationTime = Kamon.timer (
    name = "akka.remote.serialization-time",
    description = "Tracks the time taken to serialize outgoing messages"
  )

  val DeserializationTime = Kamon.timer (
    name = "akka.remote.deserialization-time",
    description = "Tracks the time taken to deserialize incoming messages"
  )



  class SerializationInstruments(systemName: String) extends InstrumentGroup(TagSet.of("system", systemName)) {
    val messageSize = register(MessageSize)
    val serializationTime = register(SerializationTime)
    val deserializationTime = register(DeserializationTime)
  }



  def recordMessageInbound(localAddress: Address, senderAddress: Option[Address], size: Long): Unit = recordMessage(localAddress, senderAddress, size, "in")

  def recordOutboundMessage(localAddress: Address, recipientAddress: Option[Address], size: Long): Unit = recordMessage(localAddress, recipientAddress, size, "out")

  private def recordMessage(localAddress: Address, peerAddress: Option[Address], size: Long, direction: String): Unit = {
    val localHost = for {
      host <- localAddress.host
      port <- localAddress.port
    } yield host+":"+port

    val peerHost = for {
      addr <- peerAddress
      host <- addr.host
      port <- addr.port
    } yield host+":"+port

    MessageSize.withTags(
      TagSet.from(
        Map(
          "system"      -> localAddress.system,
          "host"        -> localHost.getOrElse(""),
          "direction"   -> direction,
          "peer-system" -> peerAddress.map(_.system).getOrElse(""),
          "peer-host"   -> peerHost.getOrElse("")
        )
      )
    ).record(size)
  }

  def recordSerialization(system: String, time: Long) = SerializationTime.withTags(
    TagSet.from(Map(
      "system" -> system,
      "direction" -> "out"
    ))
  ).record(time)

  def recordDeserialization(system: String, time: Long) = SerializationTime.withTags(
    TagSet.from(
      Map(
        "system" -> system,
        "direction" -> "in"
      )
    )
  ).record(time)



}
