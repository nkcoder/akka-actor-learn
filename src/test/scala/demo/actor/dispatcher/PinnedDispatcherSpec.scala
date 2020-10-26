package demo.actor.dispatcher

import akka.actor.testkit.typed.scaladsl.{LoggingTestKit, ScalaTestWithActorTestKit}
import akka.actor.typed.DispatcherSelector
import demo.actor.dispatcher.DispatcherFromConfigDemo.child
import org.scalatest.wordspec.AnyWordSpecLike

class PinnedDispatcherSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "child actor" should {
    "receive message in pinned dispatcher" in {
      val childActor = testKit.spawn(child, name = "child", DispatcherSelector.fromConfig(path = "my-pinned-dispatcher"))
      LoggingTestKit.info("receive message: hi").expect {
        childActor ! "hi"
      }
    }
  }


}
