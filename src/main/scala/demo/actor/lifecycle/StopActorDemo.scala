package demo.actor.lifecycle

import org.slf4j.Logger

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object StopActorDemo {

  import akka.actor.typed.scaladsl.Behaviors
  import akka.actor.typed.{ActorSystem, Behavior, PostStop}

  object MasterControlProgram {

    sealed trait Command

    final case class SpawnJob(name: String) extends Command

    case object GracefulShutdown extends Command

    // Predefined cleanup operation
    def cleanup(log: Logger): Unit = log.info("Cleaning up!")

    def apply(): Behavior[Command] = {
      Behaviors
        .receive[Command] { (context, message) =>
          message match {
            case SpawnJob(jobName) =>
              context.log.info("Spawning job {}!", jobName)
              context.spawn(Job(jobName), name = jobName)
              Behaviors.same
            case GracefulShutdown =>
              context.log.info("Initiating graceful shutdown...")
              // perform graceful stop, executing cleanup before final system termination
              // behavior executing cleanup is passed as a parameter to Actor.stopped
              Behaviors.stopped { () =>
                cleanup(context.system.log)
              }
          }
        }
        .receiveSignal {
          case (context, PostStop) =>
            context.log.info("Master Control Program stopped")
            Behaviors.same
        }
    }
  }

  object Job {

    sealed trait Command

    def apply(name: String): Behavior[Command] = {
      Behaviors.receiveSignal[Command] {
        case (context, PostStop) =>
          context.log.info("Worker {} stopped", name)
          Behaviors.same
      }
    }
  }

  def main(args: Array[String]): Unit = {
    import MasterControlProgram._

    val system: ActorSystem[Command] = ActorSystem(MasterControlProgram(), "B7700")

    system ! SpawnJob("a")
    system ! SpawnJob("b")

    Thread.sleep(100)

    // gracefully stop the system
    system ! GracefulShutdown

    Thread.sleep(100)

    Await.result(system.whenTerminated, 3.seconds)

  }

}
