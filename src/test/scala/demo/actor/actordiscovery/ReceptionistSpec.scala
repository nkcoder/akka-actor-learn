package demo.actor.actordiscovery

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.receptionist.Receptionist
import demo.actor.actordiscovery.ReceptionistDemo.PingService
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ReceptionistSpec extends AnyWordSpec with BeforeAndAfterAll with Matchers {
  val testKit: ActorTestKit = ActorTestKit()

  "test kit system" should {
    "receive Listing" in {
      testKit.spawn(PingService())
//      testKit.system.receptionist ! Receptionist.subscribe(PingService.pingServiceKey, testKit.system)
    }
  }

}
