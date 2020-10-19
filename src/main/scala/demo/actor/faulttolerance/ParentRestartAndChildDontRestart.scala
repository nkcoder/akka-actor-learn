package demo.actor.faulttolerance

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy}

/** It is possible to override this so that child actors are not influenced when the parent actor is
  * restarted. The restarted parent instance will then have the same children as before the failure.
  *
  * If child actors are created from setup like in the previous example and they should remain
  * intact (not stopped) when parent is restarted the supervise should be placed inside the setup
  * and using SupervisorStrategy.restart.withStopChildren(false)
  */
object ParentRestartAndChildDontRestart {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(parent, "parent-restart-and-child-dont-restart")
    actorSystem ! "hello world"
    actorSystem ! "come-on"
    actorSystem ! "your turn"
  }

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

  def parent: Behavior[String] =
    Behaviors.setup { ctx =>
      val child1 = ctx.spawn(child(0), "child1")
      val child2 = ctx.spawn(child(0), "child2")

      Behaviors
        .supervise(Behaviors.receiveMessage[String] { msg =>
          // message handling that mightthrow an exception
          val parts = msg.split(" ")
          child1 ! parts(0)
          child2 ! parts(1)
          Behaviors.same
        })
        // if use `SupervisorStrategy.restart` only, child will stop.
        .onFailure(SupervisorStrategy.restart.withStopChildren(enabled = false))
    }
}
