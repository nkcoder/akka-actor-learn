package demo.actor.faulttolerance

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import demo.actor.faulttolerance.BubbleFailuresDemo.Protocol.Hello
import demo.actor.faulttolerance.BubbleFailuresDemo.Worker
import org.scalatest.wordspec.AnyWordSpecLike

class BubbleFailuresSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Worker" should {
    "reply when receiving Hello" in {
      val probe = testKit.createTestProbe[String]()
      val worker = testKit.spawn(Worker(), name = "worker-1")
      worker ! Hello("hello", probe.ref)
      probe.expectMessage(obj = "hello")
    }
  }

}
