package demo.actor.faulttolerance

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors
import demo.actor.faulttolerance.BubbleFailuresDemo.Protocol.Fail

/** For a parent to be notified when a child is terminated it has to watch the child. If the child
  * was stopped because of a failure the ChildFailed signal will be received which will contain the
  * cause. ChildFailed extends Terminated so if your use case does not need to distinguish between
  * stopping and failing you can handle both cases with the Terminated signal.
  *
  * If the parent in turn does not handle the Terminated message it will itself fail with an
  * akka.actor.typed.DeathPactException. This means that a hierarchy of actors can have a child
  * failure bubble up making each actor on the way stop but informing the top-most parent that there
  * was a failure and how to deal with it, however, the original exception that caused the failure
  * will only be available to the immediate parent out of the box (this is most often a good thing,
  * not leaking implementation details).
  *
  * There might be cases when you want the original exception to bubble up the hierarchy, this can
  * be done by handling the Terminated signal, and rethrowing the exception in each actor.
  */
object BubbleFailuresDemo {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(Boss(), "bubble-failures-demo")
    actorSystem ! Fail("I'm failed.")
//    actorSystem.terminate()
  }

  object Protocol {
    sealed trait Command
    case class Fail(text: String) extends Command
    case class Hello(text: String, replyTo: ActorRef[String]) extends Command
  }

  import Protocol._

  object Worker {
    def apply(): Behavior[Command] =
      Behaviors.receiveMessage {
        case Fail(text) =>
          throw new RuntimeException(text)
        case Hello(text, replyTo) =>
          replyTo ! text
          Behaviors.same
      }
  }

  object MiddleManagement {
    def apply(): Behavior[Command] =
      Behaviors.setup[Command] { context =>
        context.log.info("Middle management starting up")
        // default supervision of child, meaning that it will stop on failure
        val child = context.spawn(Worker(), "child")
        // we want to know when the child terminates, but since we do not handle
        // the Terminated signal, we will in turn fail on child termination
        context.watch(child)

        // here we don't handle Terminated at all which means that
        // when the child fails or stops gracefully this actor will
        // fail with a DeathPactException
        Behaviors.receiveMessage { message =>
          context.log.info("Middle management receive: {}", message)
          child ! message
          Behaviors.same
        }
      }
  }

  object Boss {
    def apply(): Behavior[Command] =
      Behaviors
        .supervise(Behaviors.setup[Command] { context =>
          context.log.info("Boss starting up")
          // default supervision of child, meaning that it will stop on failure
          val middleManagement = context.spawn(MiddleManagement(), "middle-management")
          context.watch(middleManagement)

          // here we don't handle Terminated at all which means that
          // when middle management fails with a DeathPactException
          // this actor will also fail
          Behaviors.receiveMessage[Command] { message =>
            middleManagement ! message
            Behaviors.same
          }
        })
        .onFailure(SupervisorStrategy.restart)
  }
}
