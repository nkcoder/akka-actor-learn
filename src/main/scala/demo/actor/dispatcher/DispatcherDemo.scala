package demo.actor.dispatcher

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors

/** Every ActorSystem will have a default dispatcher that will be used in case nothing else is
  * configured for an Actor. The default dispatcher can be configured, and is by default a
  * Dispatcher with the configured akka.actor.default-dispatcher.executor. If no executor is
  * selected a “fork-join-executor” is selected, which gives excellent performance in most cases.
  *
  * A default dispatcher is used for all actors that are spawned without specifying a custom
  * dispatcher. This is suitable for all actors that don’t block.
  */
object DispatcherDemo {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(guardian, "mailbox-demo")
    actorSystem.terminate()
  }

  def guardian: Behavior[String] = Behaviors.setup { context =>
    context.log.info("guardian starting.")
    // A default dispatcher is used for all actors that are spawned without specifying a custom
    // dispatcher. This is suitable for all actors that don’t block.
    val childActor = context.spawn(child, "child", DispatcherSelector.default())

    (1 to 5).foreach(i => childActor ! s"hello, $i")

    Behaviors.same
  }

  def child: Behavior[String] = Behaviors.receive[String] { (context, message) =>
    {
      context.log.info("receive message: {}", message)
      Behaviors.same
    }
  }

}
