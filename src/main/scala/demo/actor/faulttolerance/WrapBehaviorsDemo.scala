package demo.actor.faulttolerance

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import demo.actor.faulttolerance.WrapBehaviorsDemo.Counter.{GetCount, Increment}

/** Supervision allows you to declaratively describe what should happen when certain types of
  * exceptions are thrown inside an actor.
  *
  * The default supervision strategy is to stop the actor if an exception is thrown. In many cases
  * you will want to further customize this behavior. To use supervision the actual Actor behavior
  * is wrapped using Behaviors.supervise. Typically you would wrap the actor with supervision in the
  * parent when spawning it as a child.
  */
object WrapBehaviorsDemo {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(Starter(), "wrap-behavior-demo")
    actorSystem.terminate()
  }

  object Counter {

    /** Each returned behavior will be re-wrapped automatically with the supervisor.
      *
      * @return
      */
    def apply(): Behavior[Command] =
      Behaviors.supervise(counter(1)).onFailure(SupervisorStrategy.restart)

    private def counter(count: Int): Behavior[Command] =
      Behaviors.receiveMessage[Command] {
        case Increment(nr: Int) =>
          counter(count + nr)
        case GetCount(replyTo) =>
          replyTo ! count
          Behaviors.same
      }

    sealed trait Command

    case class Increment(nr: Int) extends Command

    case class GetCount(replyTo: ActorRef[Int]) extends Command
  }

  object Starter {
    def apply(): Behavior[Int] = Behaviors.setup { context =>
      {
        val counter = context.spawn(Counter(), "counter")
        counter ! Increment(nr = 10)
        counter ! GetCount(replyTo = context.self)
        Behaviors.receiveMessage { totalCount =>
          context.log.info("total count: {}", totalCount)
          Behaviors.stopped
        }
      }
    }
  }

}
