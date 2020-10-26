package demo.actor.dispatcher

import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/** Without any further configuration the default demo.actor.dispatcher runs this actor along with all other
  * actors. This is very efficient when all actor message processing is non-blocking. When all of
  * the available threads are blocked, however, then all the actors on the same demo.actor.dispatcher will
  * starve for threads and will not be able to process incoming messages.
  */
object BadFutureBlockingDemo {

  def main(args: Array[String]): Unit = {

    val root = Behaviors.setup[Nothing] { context =>
      for (i <- 1 to 100) {
        context.spawn(BlockingActor(), s"futureActor-$i") ! i
        context.spawn(PrintActor(), s"printActor-$i") ! i
      }
      Behaviors.empty
    }
    ActorSystem[Nothing](root, "BlockingDispatcherSample")
  }

  /** When facing this, you may be tempted to wrap the blocking call inside a Future and work with
    * that instead, but this strategy is too simplistic: you are quite likely to find bottlenecks or
    * run out of memory or threads when the application runs under increased load.
    *
    * Using context.executionContext as the demo.actor.dispatcher on which the blocking Future executes can
    * still be a problem, since this demo.actor.dispatcher is by default used for all other actor processing
    * unless you set up a separate demo.actor.dispatcher for the actor.
    */
  object BlockingActor {
    def apply(): Behavior[Int] =
      Behaviors.setup { context =>
        implicit val executionContext: ExecutionContextExecutor = context.system.executionContext

        Behaviors.receiveMessage[Int] { i =>
          triggerFutureBlockingOperation(i)
          Behaviors.same
        }
      }

    def triggerFutureBlockingOperation(
        i: Int
    )(implicit executionContext: ExecutionContext): Future[Unit] = {
      Future {
        //block for 5 seconds, representing blocking I/O, etc
        TimeUnit.SECONDS.sleep(5)
        println(s"Blocking operation finished: $i")
      }
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
