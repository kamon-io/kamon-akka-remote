package akka.kamon.instrumentation

import akka.actor.ActorRef
import kamon.Kamon
import kamon.context.Context
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation._

@Aspect
class MessageBufferInstrumentation {

  @Around("execution( akka.util.MessageBuffer akka.util.MessageBuffer.append(..)) && args(message, ref)")
  def aroundAppend(pjp: ProceedingJoinPoint, message: Any, ref: ActorRef): Any =
    pjp.proceed(Array(MessageWithContext(message, Kamon.currentContext()), ref))

  @Around("execution( * akka.util.MessageBuffer.head())")
  def aroundHead(pjp: ProceedingJoinPoint): (Any, ActorRef) = {
    val result = pjp.proceed().asInstanceOf[(MessageWithContext, ActorRef)]
    if (result._1 == null) result
    else (result._1.msg, result._2)
  }

  @Around("execution( * akka.util.MessageBuffer.getHead())")
  def aroundGetHead(pjp: ProceedingJoinPoint): akka.japi.Pair[Any, ActorRef] = {
    val result = pjp.proceed().asInstanceOf[akka.japi.Pair[MessageWithContext, ActorRef]]
    if (result.first == null) result.asInstanceOf[akka.japi.Pair[Any, ActorRef]]
    else
      akka.japi.Pair.create(result.first.msg, result.second)
  }

  @Around("execution( * akka.util.MessageBuffer.foreach(..)) && args(f)")
  def aroundForeach(pjp: ProceedingJoinPoint, f: (Any, ActorRef) ⇒ Unit): Unit = {
    val withContext: (Any, ActorRef) ⇒ Unit = (messageWithContext, ref) => {
      Kamon.withContext(messageWithContext.asInstanceOf[MessageWithContext].context) {
        f(messageWithContext.asInstanceOf[MessageWithContext].msg, ref)
      }
    }
    pjp.proceed(Array(withContext))
  }
}

class MessageWithContext(val msg: Any, val context: Context)
object MessageWithContext {
  def apply(msg: Any, context: Context): MessageWithContext =
    new MessageWithContext(msg, context)
}
