package kamon.akka

import akka.actor.Actor
import akka.util.MessageBuffer
import kamon.Kamon
import kamon.context.Context
import org.scalatest.{Matchers, WordSpec}

class MessageBufferTest extends WordSpec with Matchers {

  "MessageBuffer" should {

    "remember current context when appending message and execute foreach function with it" in {

      val messageBuffer = MessageBuffer.empty
      val key = Context.key("some_key", "")

      Kamon.withContext(Context.of(key, "some_value")) {
        messageBuffer.append("scala", Actor.noSender)
      }

      Kamon.currentContext().get(key) shouldBe ""

      var iterated = false
      messageBuffer.foreach { (msg, ref) =>
        iterated = true
        Kamon.currentContext().get(key) shouldBe "some_value"
      }
      iterated shouldBe true

    }
  }

}
