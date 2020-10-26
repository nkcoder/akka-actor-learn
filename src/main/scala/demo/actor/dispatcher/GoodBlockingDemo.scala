package demo.actor.dispatcher

import java.util.concurrent.TimeUnit

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.{ExecutionContext, Future}

/** An efficient method of isolating the blocking behavior, such that it does not impact the rest of
  * the system, is to prepare and use a dedicated demo.actor.dispatcher for all those blocking operations. This
  * technique is often referred to as “bulk-heading” or simply “isolating blocking”.
  *
  * This is the recommended way of dealing with any kind of blocking in reactive applications. he
  * non-exhaustive list of adequate solutions to the “blocking problem” includes the following
  * suggestions:
  *
  *   - Do the blocking call within a Future, ensuring an upper bound on the number of such calls at
  *     any point in time (submitting an unbounded number of tasks of this nature will exhaust your
  *     memory or thread limits).
  *   - Do the blocking call within a Future, providing a thread pool with an upper limit on the
  *     number of threads which is appropriate for the hardware on which the application runs, as
  *     explained in detail in this section.
  *   - Dedicate a single thread to manage a set of blocking resources (e.g. a NIO selector driving
  *     multiple channels) and dispatch events as they occur as actor messages.
  *   - Do the blocking call within an actor (or a set of actors) managed by a router, making sure
  *     to configure a thread pool which is either dedicated for this purpose or sufficiently sized.
  */
object GoodBlockingDemo {

  def main(args: Array[String]): Unit = {
    val root = Behaviors.setup[Nothing] { context =>
      for (i <- 1 to 100) {
        context.spawn(
          BlockingActor(),
          s"futureActor-$i"
        ) ! i
        context.spawn(PrintActor(), s"printActor-$i") ! i
      }
      Behaviors.empty
    }
    ActorSystem[Nothing](root, "BlockingDispatcherSample")
  }

  object BlockingActor {
    def apply(): Behavior[Int] =
      Behaviors.receive { (context, message) =>
        implicit val executionContext: ExecutionContext =
          context.system.dispatchers.lookup(
            DispatcherSelector.fromConfig(path = "my-blocking-dispatcher")
          )

        triggerFutureBlockingOperation(message)

        Behaviors.same
      }

    def triggerFutureBlockingOperation(
        i: Int
    )(implicit executionContext: ExecutionContext): Future[Unit] = {
      Future {
        //block for 5 seconds, representing blocking I/O, etc
        TimeUnit.SECONDS.sleep(5)
        println(s"Blocking operation finished for: ", i)
      }
    }
  }

  object PrintActor {
    def apply(): Behavior[Integer] =
      Behaviors.receive { (context, message) =>
        context.log.info("PrintActor: {}", message)
        Behaviors.same
      }
  }

}
