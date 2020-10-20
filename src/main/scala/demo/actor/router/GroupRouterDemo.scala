package demo.actor.router

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

/** The group router is created with a ServiceKey and uses the receptionist (see Receptionist) to
  * discover available actors for that key and routes messages to one of the currently known
  * registered actors for a key.
  */
object GroupRouterDemo {
  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](guardianBehavior = guardian, name = "group-router-demo")
  }

  def guardian: Behavior[Nothing] = Behaviors.setup[Nothing] {
    val serviceKey = ServiceKey[Worker.Command](id = "log_worker")
    context => {
      val worker = context.spawn(behavior = Worker(), name = "worker")
      val worker2 = context.spawn(behavior = Worker(), name = "worker2")
      context.system.receptionist ! Receptionist.register(serviceKey, worker)
      context.system.receptionist ! Receptionist.register(serviceKey, worker2)
      // Randomly selects a routee when a message is sent through the router.
      // This is the default for group routers as the group of routees is expected to change as
      // nodes join and leave the cluster.
      //
      // An optional parameter preferLocalRoutees can be used for this strategy. Routers will only
      // use routees located in local actor system if preferLocalRoutees is true and local routees
      // do exist. The default value for this parameter is false.
      val group = Routers
        .group(serviceKey)
        .withRandomRouting(preferLocalRoutees = true)
      val router = context.spawn(group, "worker-group", DispatcherSelector.sameAsParent())

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
