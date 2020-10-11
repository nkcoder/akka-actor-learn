package demo.actor.interaction

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}

/**
  * Tell is asynchronous which means that the method returns right away.
  * After the statement is executed there is no guarantee that the message has been processed by the recipient yet.
  * It also means there is no way to know if the message was received, the processing succeeded or failed.
  *
  * Useful when:
  *
  * - It is not critical to be sure that the message was processed
  * - There is no way to act on non successful delivery or processing
  * - We want to minimize the number of messages created to get higher throughput (sending a response would require
  * creating twice the number of messages)
  *
  * Problems:
  *
  * - If the inflow of messages is higher than the actor can process the inbox will fill up and can in the worst case
  * cause the JVM crash with an OutOfMemoryError
  * - If the message gets lost, the sender will not know
  */
object FireForgetDemo {

  def main(args: Array[String]): Unit = {
    import Printer._
    val system: ActorSystem[PrintMe] = ActorSystem(Printer(), "fire_and_forget")

    system ! PrintMe("page one")
    system ! PrintMe("page two")

    system.terminate()
  }

  object Printer {

    def apply(): Behavior[PrintMe] = {
      Behaviors.receive { case (context, PrintMe(message)) =>
        context.log.info("print message: {}", message)
        Behaviors.same
      }
    }

    final case class PrintMe(message: String)

  }

}
