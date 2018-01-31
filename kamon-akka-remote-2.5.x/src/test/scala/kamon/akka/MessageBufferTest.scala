package kamon.akka

import akka.actor.{Actor, ActorRef}
import akka.japi.Pair
import akka.japi.function.Procedure2
import akka.util.MessageBuffer
import org.scalatest.{FlatSpec, Matchers}

class MessageBufferTest extends FlatSpec with Matchers {

  behavior of "MessageBuffer"

  it should "allow storing message and calling foreach to get it back" in {

    val messageBuffer = MessageBuffer.empty.append("scala", Actor.noSender)
    var iterated = false
    messageBuffer.foreach { (msg, ref) =>
      {
        iterated = true
        msg shouldBe "scala"
        ref shouldBe Actor.noSender
      }
    }
    iterated shouldBe true

  }

  it should "allow storing message and calling forEach to get it back" in {

    val messageBuffer = MessageBuffer.empty.append("java", Actor.noSender)
    var iterated = false
    messageBuffer.forEach(new Procedure2[Any, ActorRef] {
      override def apply(msg: Any, ref: ActorRef): Unit = {
        iterated = true
        msg shouldBe "java"
        ref shouldBe Actor.noSender
      }
    })

    iterated shouldBe true

  }

  it should "allow storing message and getting it back using head method" in {
    val messageBuffer = MessageBuffer.empty
    messageBuffer.append("scala", Actor.noSender)
    messageBuffer.head shouldBe ("scala", Actor.noSender)
  }

  it should "allow storing message and getting it back using getHead method" in {
    val messageBuffer = MessageBuffer.empty
    messageBuffer.append("java", Actor.noSender)
    messageBuffer.getHead shouldBe Pair.create("java", Actor.noSender)
  }

  it should "allow calling head without appended messages" in {
    val messageBuffer = MessageBuffer.empty
    messageBuffer.head shouldBe (null, null)
  }

  it should "allow calling getHead without appended messages" in {
    val messageBuffer = MessageBuffer.empty
    messageBuffer.getHead shouldBe Pair.create(null, null)
  }

}
