package demo.actor.faulttolerance

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, PreRestart, SupervisorStrategy}

/** Child actors are often started in a setup block that is run again when the parent actor is
  * restarted. The child actors are stopped to avoid resource leaks of creating new child actors
  * each time the parent is restarted.
  */
object ParentRestartAndChildRestart {
  def child(size: Long): Behavior[String] =
    Behaviors
      .receive[String] { (context, msg) =>
        context.log.info("receive message: {}", msg)
        child(size + msg.length)
      }
      .receiveSignal { case (context, signal) =>
        context.log.info("receive signal: {}", signal)
        Behaviors.same
      }

  def parent: Behavior[String] = {
    Behaviors
      .supervise[String] {
        Behaviors.setup { ctx =>
          val child1 = ctx.spawn(child(0), "child1")
          val child2 = ctx.spawn(child(0), "child2")

          Behaviors
            .receiveMessage[String] { msg =>
              // message handling that might throw an exception
              val parts = msg.split(" ")
              child1 ! parts(0)
              child2 ! parts(1)
              Behaviors.same
            }
            .receiveSignal {
              // Before a supervised actor is restarted it is sent the PreRestart signal giving it
              // a chance to clean up resources it has created
              case (context, signal) if signal == PreRestart =>
                context.log.info("receive signal: {}", signal)
                Behaviors.same
            }
        }
      }
      .onFailure(SupervisorStrategy.restart)
  }

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(parent, "parent-restart-and-child-restart")
    actorSystem ! "hello world"
    actorSystem ! "come-on"
    actorSystem ! "your turn"
  }
}
