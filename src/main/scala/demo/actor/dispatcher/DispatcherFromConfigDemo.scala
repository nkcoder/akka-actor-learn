package demo.actor.dispatcher

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

/** Dispatcher
  *
  * This is an event-based dispatcher that binds a set of Actors to a thread pool. The default
  * dispatcher is used if no other is specified.
  *
  *   - Shareability: Unlimited
  *   - Mailboxes: Any, creates one per Actor
  *   - Use cases: Default dispatcher, Bulkheading
  *   - Driven by: java.util.concurrent.ExecutorService. Specify using “executor” using
  *     “fork-join-executor”, “thread-pool-executor” or the fully-qualified class name of an
  *     akka.dispatcher.ExecutorServiceConfigurator implementation.
  */
object DispatcherFromConfigDemo {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(guardian, "mailbox-demo")
    actorSystem.terminate()
  }

  def guardian: Behavior[String] = Behaviors.setup { context =>
    context.log.info("guardian starting.")
    // load dispatcher from config
    val childActor = context.spawn(
      child,
      name = "child",
      DispatcherSelector.fromConfig(path = "my-special-dispatcher")
    )

    (6 to 10) foreach { i => childActor ! s"hello, $i" }

    Behaviors.same
  }

  def child: Behavior[String] = Behaviors.receive[String] { (context, message) =>
    {
      context.log.info("receive message: {}", message)
      Behaviors.same
    }
  }

}
