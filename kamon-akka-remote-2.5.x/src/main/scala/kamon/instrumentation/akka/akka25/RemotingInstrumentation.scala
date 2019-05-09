package kamon.instrumentation.akka.akka25

import akka.actor.ActorSystem
import akka.kamon.instrumentation.kanela.interceptor.AkkaPduProtobufCodecConstructMessageMethodInterceptor
import akka.remote.kamon.instrumentation.kanela.advisor._
import kamon.Kamon
import kamon.context.Storage.Scope
import kamon.instrumentation.akka.AkkaRemoteMetrics.SerializationInstruments
import kamon.instrumentation.context.{CaptureCurrentContextOnExit, HasContext}
import kanela.agent.api.instrumentation.InstrumentationBuilder
import kanela.agent.libs.net.bytebuddy.asm.Advice


class RemotingInstrumentation extends InstrumentationBuilder {

  /**
    * Send messages might be buffered if they reach the EndpointWriter before it has been initialized and the current
    * Context might be lost after the buffering, so we make sure we capture the context when the Send command was
    * created and then apply it during the EndpointWrite.writeSend method execution (see bellow).
    */
  onType("akka.remote.EndpointManager.Send")
    .mixin(classOf[HasContext.Mixin])
    .advise(isConstructor, CaptureCurrentContextOnExit)

  onType("akka.remote.EndpointWriter")
    .advise(method("writeSend"), WriteSendWithContext)


  /**
    * Reads and writes the Akka PDU using a modified version of the Protobuf that has an extra field for a Context
    * instance.
    */
  onType("akka.remote.transport.AkkaPduProtobufCodec$")
    .intercept(method("constructMessage"), new AkkaPduProtobufCodecConstructMessageMethodInterceptor())
    .advise(method("decodeMessage"), classOf[AkkaPduProtobufCodecDecodeMessage])


  /**
    * Mixin Serialization Instruments to the Actor System and use them to record the serialization and deserialization
    * time metrics.
    */
  onType("akka.actor.ActorSystem")
    .mixin(classOf[HasSerializationInstruments.Mixin])
    .advise(isConstructor, HasSerializationInstruments)

  onType("akka.remote.MessageSerializer$")
    .advise(method("serialize"), MeasureSerializationTime)
    .advise(method("deserialize"), MeasureDeserializationTime)

}

object WriteSendWithContext {

  @Advice.OnMethodEnter(suppress = classOf[Throwable])
  def enter(@Advice.Argument(0) send: HasContext): Scope = {
    Kamon.store(send.context)
  }

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def exit(@Advice.Enter scope: Scope): Unit = {
    scope.asInstanceOf[Scope].close()
  }
}

trait HasSerializationInstruments {
  def serializationInstruments: SerializationInstruments
  def setSerializationInstruments(instruments: SerializationInstruments): Unit
}

object HasSerializationInstruments {

  class Mixin(var serializationInstruments: SerializationInstruments) extends HasSerializationInstruments {
    override def setSerializationInstruments(instruments: SerializationInstruments): Unit =
      serializationInstruments = instruments
  }

  @Advice.OnMethodExit(suppress = classOf[Throwable])
  def exit(@Advice.This system: ActorSystem with HasSerializationInstruments): Unit =
    system.setSerializationInstruments(new SerializationInstruments(system.name))

}

object MeasureSerializationTime {

  @Advice.OnMethodEnter
  def enter(): Long = {
    System.nanoTime()
  }

  @Advice.OnMethodExit
  def exit(@Advice.Argument(0) system: AnyRef, @Advice.Enter startNanoTime: Long): Unit =
    system.asInstanceOf[HasSerializationInstruments]
      .serializationInstruments
      .serializationTime
      .record(startNanoTime - System.nanoTime())
}

object MeasureDeserializationTime {

  @Advice.OnMethodEnter
  def enter(): Long = {
    System.nanoTime()
  }

  @Advice.OnMethodExit
  def exit(@Advice.Argument(0) system: AnyRef, @Advice.Enter startNanoTime: Long): Unit = {
    system.asInstanceOf[HasSerializationInstruments]
      .serializationInstruments
      .deserializationTime
      .record(startNanoTime - System.nanoTime())
  }
}
