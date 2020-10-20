package demo.actor.mailbox

import akka.actor.typed.{ActorSystem, Behavior, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors

/** Each actor in Akka has a Mailbox, this is where the messages are enqueued before being processed
  * by the actor.
  *
  * By default an unbounded mailbox is used, this means any number of messages can be enqueued into
  * the mailbox.
  *
  * The unbounded mailbox is a convenient default but in a scenario where messages are added to the
  * mailbox faster than the actor can process them, this can lead to the application running out of
  * memory. For this reason a bounded mailbox can be specified, the bounded mailbox will pass new
  * messages to deadletters when the mailbox is full.
  */
object MailboxDemo {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(guardian, "mailbox-demo")
    actorSystem.terminate()
  }

  def guardian: Behavior[String] = Behaviors.setup { context =>
    val capacity = 5
    val childActor = context.spawn(child, "child", MailboxSelector.bounded(capacity))

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
