package akka.remote.kamon.instrumentation.kanela.advisor

import akka.actor.{Address, AddressFromURIString, ExtendedActorSystem}
import akka.dispatch.sysmsg.SystemMessage
import akka.remote.ContextAwareWireFormats.AckAndContextAwareEnvelopeContainer
import kamon.Kamon
import kamon.akka.context.ContextContainer
import kamon.context.Storage.Scope
import akka.remote.RemoteActorRefProvider
import akka.util.ByteString
import kamon.akka.RemotingMetrics
import kamon.context.BinaryPropagation.ByteStreamReader
import kanela.agent.libs.net.bytebuddy.asm.Advice.{Argument, Enter, OnMethodEnter, OnMethodExit, This}

/**
  * Advisor for akka.remote.EndpointManager$Send::constructor
  */
class SendConstructorAdvisor
object SendConstructorAdvisor {
  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@This node: HasContext): Unit = {
    node.context // forces initialization on the calling thread.
  }
}

/**
  * Advisor for akka.remote.EndpointWriter::writeSend
  */
class EndpointWriterWriteSendMethodAdvisor
object EndpointWriterWriteSendMethodAdvisor {
  @OnMethodEnter(suppress = classOf[Throwable])
  def onEnter(@Argument(0) send: HasContext): Scope = {
    Kamon.storeContext(send.context)
  }

  @OnMethodExit(suppress = classOf[Throwable])
  def onExit(@Enter scope: Scope): Unit = {
    scope.close()
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
        Kamon.storeContext(ctx)
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
