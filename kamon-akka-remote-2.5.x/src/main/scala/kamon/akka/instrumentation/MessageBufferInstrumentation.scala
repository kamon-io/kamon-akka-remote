package akka.kamon.instrumentation

import kamon.Kamon
import kamon.context.Context

trait InstrumentedMessageBufferNode {
  def context: Context
}

object InstrumentedMessageBufferNode {
  def apply(): InstrumentedMessageBufferNode = new InstrumentedMessageBufferNode {
    val context = Kamon.currentContext()
  }
}