package akka.remote.kamon.instrumentation.kanela.advisor

import akka.actor.{Address, AddressFromURIString}
import akka.dispatch.sysmsg.SystemMessage
import akka.remote.ContextAwareWireFormats.AckAndContextAwareEnvelopeContainer
import akka.remote.EndpointManager.Send
import kamon.Kamon
import kamon.context.Storage.Scope
import akka.remote.RemoteActorRefProvider
import akka.util.ByteString
import kamon.akka.RemotingMetrics
import kamon.context.BinaryPropagation.ByteStreamReader
import kamon.instrumentation.akka.akka25.mixin.ContextContainer
import kanela.agent.libs.net.bytebuddy.asm.Advice
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, Enter, OnMethodEnter, OnMethodExit, This}

/**
  * Advisor for akka.remote.EndpointManager$Send::constructor
  */
class SendConstructorAdvisor
object SendConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: ContextContainer): Unit = {
    node.context // forces initialization on the calling thread.
  }
}

/**
  * Advisor for akka.remote.EndpointWriter::writeSend
  */
object EndpointWriterWriteSendMethodAdvisor {

  @OnMethodEnter
  def onEnter(@Advice.Argument(0) send: Send): Scope = {
    Kamon.store(send.asInstanceOf[ContextContainer].context)
  }

  @OnMethodExit
  def onExit(@Advice.Enter scope: Scope): Unit = {
    scope.asInstanceOf[Scope].close()
  }
}


/**
  * Advisor for akka.actor.ActorCell::sendSystemMessage
  * Advisor for akka.actor.UnstartedCell::sendSystemMessage
  */
class SendSystemMessageMethodAdvisor
object SendSystemMessageMethodAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@Argument(0) msg: SystemMessage): Unit = {
    msg.asInstanceOf[ContextContainer].setContext(Kamon.currentContext())
  }
}

/**
  * Advisor for akka.remote.transport.AkkaPduProtobufCodec$::decodeMessage
  */
class AkkaPduProtobufCodecDecodeMessageMethodAdvisor
object AkkaPduProtobufCodecDecodeMessageMethodAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@Argument(0) bs: ByteString,
              @Argument(1) provider: RemoteActorRefProvider,
              @Argument(2) localAddress: Address): Unit = {
    val ackAndEnvelope = AckAndContextAwareEnvelopeContainer.parseFrom(bs.toArray)
    if (ackAndEnvelope.hasEnvelope && ackAndEnvelope.getEnvelope.hasTraceContext) {
      val remoteCtx = ackAndEnvelope.getEnvelope.getTraceContext

      if(remoteCtx.getContext.size() > 0) {
        val ctx = Kamon.defaultBinaryPropagation().read(
        ByteStreamReader.of(remoteCtx.getContext.toByteArray)
        )
        Kamon.store(ctx)
      }

      RemotingMetrics.recordMessageInbound(
      localAddress  = localAddress,
      senderAddress = {
        val senderPath = ackAndEnvelope.getEnvelope.getSender.getPath
        if(senderPath.isEmpty) None else Some(AddressFromURIString(senderPath))
      },
      size = ackAndEnvelope.getEnvelope.getMessage.getMessage.size()
      )
    }
  }
}
