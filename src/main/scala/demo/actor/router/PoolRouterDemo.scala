package demo.actor.router

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector, SupervisorStrategy}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

/** The pool router is created with a routee Behavior and spawns a number of children with that
  * behavior which it will then forward messages to.
  *
  * If a child is stopped the pool router removes it from its set of routees. When the last child
  * stops the router itself stops. To make a resilient router that deals with failures the routee
  * Behavior must be supervised.
  *
  * As actor children are always local the routees are never spread across a cluster with a pool
  * router.
  */
object PoolRouterDemo {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](guardian, "pool-router-demo")
  }

  def guardian: Behavior[Nothing] = Behaviors.setup[Nothing] {
    val poolSize: Int = 3

    context => {
      // Since the router itself is spawned as an actor the dispatcher used for it can be configured
      // directly in the call to spawn. The routees, however, are spawned by the router. Therefore,
      // the PoolRouter has a property to configure the Props of its routees
      val pool = Routers
        .pool(poolSize)(
          // make sure the workers are restarted if they fail
          Behaviors.supervise(Worker()).onFailure[Exception](SupervisorStrategy.restart)
        )
        .withRouteeProps(DispatcherSelector.blocking())
      val router = context.spawn(pool, "worker-pool", DispatcherSelector.sameAsParent())

      (0 to 10).foreach { n =>
        router ! Worker.DoLog(s"msg $n")
      }

      Behaviors.same

    }
  }

  object Worker {
    def apply(): Behavior[Command] = Behaviors.setup { context =>
      context.log.info("Starting worker")

      Behaviors.receiveMessage { case DoLog(text) =>
        context.log.info("Got message {}", text)
        Behaviors.same
      }
    }

    sealed trait Command

    final case class DoLog(text: String) extends Command
  }

}
