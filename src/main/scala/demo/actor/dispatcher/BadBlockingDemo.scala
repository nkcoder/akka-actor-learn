package demo.actor.dispatcher

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

/** Without any further configuration the default demo.actor.dispatcher runs this actor along with all other
  * actors. This is very efficient when all actor message processing is non-blocking. When all of
  * the available threads are blocked, however, then all the actors on the same demo.actor.dispatcher will
  * starve for threads and will not be able to process incoming messages.
  */
object BadBlockingDemo {

  def main(args: Array[String]): Unit = {

    /** PrintActor is considered non-blocking, however it is not able to proceed with handling the
      * remaining messages, since all the threads are occupied and blocked by the other blocking
      * actors - thus leading to thread starvation.
      */
    val root = Behaviors.setup[Nothing] { context =>
      for (i <- 1 to 100) {
        context.spawn(BlockingActor(), s"futureActor-$i") ! i
        context.spawn(PrintActor(), s"printActor-$i") ! i
      }
      Behaviors.empty
    }
    ActorSystem[Nothing](root, "BlockingDispatcherSample")
  }

  object BlockingActor {
    def apply(): Behavior[Int] =
      Behaviors.receiveMessage { i =>
        // DO NOT DO THIS HERE: this is an example of incorrect code,
        // better alternatives are described further on.

        //block for 5 seconds, representing blocking I/O, etc
        Thread.sleep(5000)
        println(s"Blocking operation finished: $i")
        Behaviors.same
      }
  }

  object PrintActor {
    def apply(): Behavior[Integer] =
      Behaviors.receiveMessage { i =>
        println(s"PrintActor: $i")
        Behaviors.same
      }
  }

}
