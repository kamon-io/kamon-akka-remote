package akka.kamon.instrumentation

import kamon.Kamon
import kamon.context.Context
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

@Aspect
class MessageBufferInstrumentation {


  @Around("execution(* akka.util.MessageBuffer.Node.apply(..)) && this(node)")
  def aroundForeach(pjp: ProceedingJoinPoint, node: InstrumentedMessageBufferNode): Any =
    Kamon.withContext(node.context)(pjp.proceed())


  @After("execution(akka.util.MessageBuffer.Node.new(..)) && this(node)")
  def afterCreatingMessageBufferNode(node: InstrumentedMessageBufferNode): Unit =
    node.context // forces initialization on the calling thread.

  @DeclareMixin("akka.util.MessageBuffer.Node")
  def mixinInstrumentationToMessageBufferNode: InstrumentedMessageBufferNode =
    InstrumentedMessageBufferNode()
}

trait InstrumentedMessageBufferNode {
  def context: Context
}

object InstrumentedMessageBufferNode {
  def apply(): InstrumentedMessageBufferNode = new InstrumentedMessageBufferNode {
    val context = Kamon.currentContext()
  }
}