package demo.actor.mailbox

import akka.actor.typed.{ActorSystem, Behavior, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors

/** fromConfig takes an absolute config path to a block defining the demo.actor.dispatcher in the config file
  */
object MailboxFromConfigDemo {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(guardian, "mailbox-demo")
    actorSystem.terminate()
  }

  def guardian: Behavior[String] = Behaviors.setup { context =>
    val childActor = context.spawn(
      child,
      name = "child",
      MailboxSelector.fromConfig(path = "my-special-mailbox")
    )

    (1 to 20).foreach(i => childActor ! s"hi, $i")

    Behaviors.same
  }

  def child: Behavior[String] = Behaviors.receive[String] { (context, message) =>
    {
      context.log.info("receive message: {}", message)
      Behaviors.same
    }
  }
}
