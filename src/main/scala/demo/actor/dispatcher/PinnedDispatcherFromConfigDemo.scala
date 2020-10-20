package demo.actor.dispatcher

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

/** PinnedDispatcher
  *
  * This dispatcher dedicates a unique thread for each actor using it; i.e. each actor will have its
  * own thread pool with only one thread in the pool.
  *
  *   - Shareability: None
  *   - Mailboxes: Any, creates one per Actor
  *   - Use cases: Bulkheading
  *   - Driven by: Any akka.dispatch.ThreadPoolExecutorConfigurator. By default a
  *     “thread-pool-executor”.
  */
object PinnedDispatcherFromConfigDemo {

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
      DispatcherSelector.fromConfig(path = "my-pinned-dispatcher")
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
