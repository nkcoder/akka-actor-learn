package demo.actor.faulttolerance

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.BehaviorInterceptor
import akka.actor.typed.scaladsl.Behaviors
import demo.actor.faulttolerance.BubbleFailuresDemo.Protocol.{Fail, Hello}
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

    "restart when sending Fail to Worker" in {
      val probe = testKit.createTestProbe[String]()
      val worker = testKit.spawn(Worker(), name = "worker-2")
      worker ! Fail("failed")
    }
  }

}
