package demo.actor.dispatcher

import akka.actor.testkit.typed.scaladsl.{ActorTestKit, LoggingTestKit}
import akka.actor.typed.{ActorSystem, DispatcherSelector}
import demo.actor.dispatcher.DispatcherDemo.child
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 * Asynchronous testing uses a real ActorSystem that allows you to test your Actors in a more realistic environment.
 * Tests create an instance of ActorTestKit. This provides access to:
 *
 * An ActorSystem
 * Methods for spawning Actors. These are created under the special testkit user guardian
 * A method to shut down the ActorSystem from the test suite
 * This first example is using the “raw” ActorTestKit but if you are using ScalaTest you can simplify the tests by using the Test framework integration. It’s still good to read this section to understand how it works.
 */
class DispatcherSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  val testKit: ActorTestKit = ActorTestKit()
  implicit val system: ActorSystem[Nothing] = testKit.system

  "child actor" should {
    "run in default dispatcher" in {
      val childActor = testKit.spawn(child, "child", DispatcherSelector.defaultDispatcher())
      LoggingTestKit.info("receive message: hello").expect {
        childActor ! "hello"
      }
    }

    "run in blocking dispatcher" in {
      val childActor = testKit.spawn(child, "child", DispatcherSelector.blocking())
      LoggingTestKit.info("receive message: hello").expect {
        childActor ! "hello"
      }
    }

    "run in the dispatcher same as parent" in {
      val childActor = testKit.spawn(child, "child", DispatcherSelector.blocking())
      LoggingTestKit.info("receive message: hello").expect {
        childActor ! "hello"
      }
    }


  }

  /**
   * Your test is responsible for shutting down the ActorSystem e.g. using BeforeAndAfterAll when using ScalaTest.
   */
  override protected def afterAll(): Unit = {
    super.afterAll()
    testKit.shutdownTestKit()
  }
}
