package akka.kamon.instrumentation

import java.io.ByteArrayOutputStream

import akka.KamonOptionVal.OptionVal
import akka.actor.{ActorRef, Address, AddressFromURIString, ExtendedActorSystem}
import akka.dispatch.sysmsg.SystemMessage
import akka.remote.WireFormats._
import akka.remote.instrumentation.ContextAwareWireFormats.{AckAndContextAwareEnvelopeContainer, ContextAwareRemoteEnvelope, RemoteContext}
import akka.remote.{Ack, RemoteActorRefProvider, SeqNo}
import akka.util.ByteString
import kamon.Kamon
import kamon.akka.RemotingMetrics
import kamon.akka.context.{ContextContainer, HasTransientContext}
import kamon.context.BinaryPropagation.{ByteStreamReader, ByteStreamWriter}
import kamon.instrumentation.Mixin.HasContext
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._




@Aspect
class HasTransientContextIntoSend {
  @DeclareMixin("akka.remote.EndpointManager.Send+")
  def mixinHasContextToSystemMessage: HasContext = HasTransientContext.fromCurrentContext()

  @Pointcut("execution(akka.remote.EndpointManager.Send+.new(..)) && this(send)")
  def sendCreationCreation(send: HasContext): Unit = {}

  @After("sendCreationCreation(send)")
  def afterSystemMessageCreation(send: HasContext): Unit = {
    // Necessary to force the initialization of HasContext at the moment of creation.
    send.context
  }


  @Pointcut("execution(* akka.remote.EndpointWriter.writeSend(*)) && args(send)")
  def writingToTransport(send: HasContext): Unit = {}

  @Around("writingToTransport(send)")
  def aroundWriteToTransport(pjp: ProceedingJoinPoint, send: HasContext) = {
    Kamon.withContext(send.context) {
      pjp.proceed()
    }
  }
}


@Aspect
class RemotingInstrumentation {
  private lazy val serializationInstrumentation = Kamon.config().getBoolean("kamon.akka-remote.serialization-metric")

  @Pointcut("execution(* akka.actor.ActorCell.sendSystemMessage(*)) && args(msg)")
  def sendSystemMessageInActorCell(msg: SystemMessage): Unit = {}

  @Pointcut("execution(* akka.actor.UnstartedCell.sendSystemMessage(*)) && args(msg)")
  def sendSystemMessageInUnstartedActorCell(msg: SystemMessage): Unit = {}

  @Before("sendSystemMessageInActorCell(msg)")
  def afterSendSystemMessageMessageInActorCell(msg: SystemMessage): Unit = applyCurrentContext(msg)

  @Before("sendSystemMessageInUnstartedActorCell(msg)")
  def afterSendSystemMessageMessageInUnstartedActorCell(msg: SystemMessage): Unit = applyCurrentContext(msg)

  private def applyCurrentContext(msg: SystemMessage): Unit = {
    msg.asInstanceOf[ContextContainer].setContext(Kamon.currentContext())
  }




  @Pointcut("execution(* akka.remote.transport.AkkaPduProtobufCodec$.constructMessage(..)) && " +
    "args(localAddress, recipient, serializedMessage, senderOption, seqOption, ackOption)")
  def constructAkkaPduMessage(localAddress: Address, recipient: ActorRef, serializedMessage: SerializedMessage,
    senderOption: OptionVal[ActorRef], seqOption: Option[SeqNo], ackOption: Option[Ack]): Unit = {}

  @Around("constructAkkaPduMessage(localAddress, recipient, serializedMessage, senderOption, seqOption, ackOption)")
  def aroundSerializeRemoteMessage(pjp: ProceedingJoinPoint, localAddress: Address, recipient: ActorRef,
    serializedMessage: SerializedMessage, senderOption: OptionVal[ActorRef], seqOption: Option[SeqNo], ackOption: Option[Ack]): AnyRef = {

    val ackAndEnvelopeBuilder = AckAndContextAwareEnvelopeContainer.newBuilder
    val envelopeBuilder = ContextAwareRemoteEnvelope.newBuilder

    envelopeBuilder.setRecipient(serializeActorRef(recipient.path.address, recipient))
    if (senderOption.isDefined)
      envelopeBuilder.setSender(serializeActorRef(localAddress, senderOption.get))
    seqOption foreach { seq ⇒ envelopeBuilder.setSeq(seq.rawValue) }
    ackOption foreach { ack ⇒ ackAndEnvelopeBuilder.setAck(ackBuilder(ack)) }
    envelopeBuilder.setMessage(serializedMessage)

    val out = new ByteArrayOutputStream()
    Kamon.defaultBinaryPropagation().write(Kamon.currentContext(), ByteStreamWriter.of(out))

    val remoteTraceContext = RemoteContext.newBuilder().setContext(
      akka.protobuf.ByteString.copyFrom(out.toByteArray)
    )
    envelopeBuilder.setTraceContext(remoteTraceContext)

    ackAndEnvelopeBuilder.setEnvelope(envelopeBuilder)

    RemotingMetrics.recordOutboundMessage(
      localAddress      = localAddress,
      recipientAddress  = Some(recipient.path.address),
      size              = envelopeBuilder.getMessage.getMessage.size()
    )

    ByteString.ByteString1C(ackAndEnvelopeBuilder.build.toByteArray) //Reuse Byte Array (naughty!)
  }

  // Copied from akka.remote.transport.AkkaPduProtobufCodec because of private access.
  private def ackBuilder(ack: Ack): AcknowledgementInfo.Builder = {
    val ackBuilder = AcknowledgementInfo.newBuilder()
    ackBuilder.setCumulativeAck(ack.cumulativeAck.rawValue)
    ack.nacks foreach { nack ⇒ ackBuilder.addNacks(nack.rawValue) }
    ackBuilder
  }

  // Copied from akka.remote.transport.AkkaPduProtobufCodec because of private access.
  private def serializeActorRef(defaultAddress: Address, ref: ActorRef): ActorRefData = {
    ActorRefData.newBuilder.setPath(
      if (ref.path.address.host.isDefined) ref.path.toSerializationFormat
      else ref.path.toSerializationFormatWithAddress(defaultAddress)).build()
  }

  // Copied from akka.remote.transport.AkkaPduProtobufCodec because of private access.
  private def serializeAddress(address: Address): AddressData = address match {
    case Address(protocol, system, Some(host), Some(port)) ⇒
      AddressData.newBuilder
        .setHostname(host)
        .setPort(port)
        .setSystem(system)
        .setProtocol(protocol)
        .build()
    case _ ⇒ throw new IllegalArgumentException(s"Address [$address] could not be serialized: host or port missing.")
  }

  @Pointcut("execution(* akka.remote.transport.AkkaPduProtobufCodec$.decodeMessage(..)) && args(bs, provider, localAddress)")
  def decodeRemoteMessage(bs: akka.util.ByteString, provider: RemoteActorRefProvider, localAddress: Address): Unit = {}

  @Around("decodeRemoteMessage(bs, provider, localAddress)")
  def aroundDecodeRemoteMessage(pjp: ProceedingJoinPoint, bs: ByteString, provider: RemoteActorRefProvider, localAddress: Address): AnyRef = {
    val ackAndEnvelope = AckAndContextAwareEnvelopeContainer.parseFrom(bs.toArray)
    if (ackAndEnvelope.hasEnvelope && ackAndEnvelope.getEnvelope.hasTraceContext) {
      val remoteCtx = ackAndEnvelope.getEnvelope.getTraceContext

      if(remoteCtx.getContext.size() > 0) {
        val ctx = Kamon.defaultBinaryPropagation().read(
          ByteStreamReader.of(remoteCtx.getContext.toByteArray)
        )
        Kamon.storeContext(ctx)
      }

      RemotingMetrics.recordMessageInbound(
        localAddress  = localAddress,
        senderAddress = {
          val senderPath = ackAndEnvelope.getEnvelope.getSender.getPath
          if(senderPath.isEmpty) None else Some(AddressFromURIString(senderPath))
        },
        size          = ackAndEnvelope.getEnvelope.getMessage.getMessage.size()
      )

    }
    pjp.proceed()
  }

  @Pointcut("execution(* akka.remote.MessageSerializer$.serialize(..)) && args(system, message)")
  def serializeMessage(system: ExtendedActorSystem, message: AnyRef): Unit = {}

  @Around("serializeMessage(system, message)")
  def aroundSerializeMessage(pjp: ProceedingJoinPoint, system: ExtendedActorSystem, message: AnyRef): AnyRef = {
    if(serializationInstrumentation) {
      val start = Kamon.clock().nanos()
      val res = pjp.proceed()
      RemotingMetrics.recordSerialization(system.name, Kamon.clock().nanos() - start)
      res
    } else {
      pjp.proceed()
    }
  }

  @Pointcut("execution(* akka.remote.MessageSerializer$.deserialize(..)) && args(system, messageProtocol)")
  def deserializeMessage(system: ExtendedActorSystem, messageProtocol: SerializedMessage): Unit = {}

  @Around("deserializeMessage(system, messageProtocol)")
  def aroundDeserializeMessage(pjp: ProceedingJoinPoint, system: ExtendedActorSystem, messageProtocol: SerializedMessage): AnyRef = {
    if(serializationInstrumentation) {
      val start = Kamon.clock().nanos()
      val res = pjp.proceed()
      RemotingMetrics.recordDeserialization(system.name, Kamon.clock().nanos() - start)
      res
    } else {
      pjp.proceed()
    }
  }
}
