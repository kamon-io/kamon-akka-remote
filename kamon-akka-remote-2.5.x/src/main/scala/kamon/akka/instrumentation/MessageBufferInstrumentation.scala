package akka.kamon.instrumentation

import kamon.Kamon
import kamon.context.Context
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

trait InstrumentedMessageBufferNode {
  def context: Context
}

object InstrumentedMessageBufferNode {
  def apply(): InstrumentedMessageBufferNode = new InstrumentedMessageBufferNode {
    val context = Kamon.currentContext()
  }
}