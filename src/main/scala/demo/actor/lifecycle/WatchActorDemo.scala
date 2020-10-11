package demo.actor.lifecycle

import java.util.concurrent.TimeUnit

import akka.actor.typed.Terminated
import org.slf4j.Logger

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object WatchActorDemo {

  import akka.actor.typed.scaladsl.Behaviors
  import akka.actor.typed.{ActorSystem, Behavior, PostStop}

  def main(args: Array[String]): Unit = {

    val system: ActorSystem[Command] = ActorSystem(MasterControlProgram(), "MasterControl")

    system ! SpawnJob("A")
    system ! SpawnJob("B")

    TimeUnit.MILLISECONDS.sleep(200)

    // gracefully stop the system
    system ! GracefulShutdown

    Thread.sleep(100)

    Await.result(system.whenTerminated, 3.seconds)
  }

  sealed trait Command

  final case class SpawnJob(name: String) extends Command

  case object GracefulShutdown extends Command

  final case object ShutdownJob extends Command

  object MasterControlProgram {

    def apply(): Behavior[Command] = {
      Behaviors
        .receive[Command] { (context, message) =>
          message match {
            case SpawnJob(jobName) =>
              context.log.info("Spawning job {}!", jobName)

              val jobRef = context.spawn(Job(jobName), name = jobName)

              context.watch(jobRef)

              jobRef ! ShutdownJob

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
          case (context, Terminated(ref)) =>
            context.log.info("child actor stopped: {}", ref.path.name)
            Behaviors.same
        }
    }

    // Predefined cleanup operation
    def cleanup(log: Logger): Unit = log.info("Cleaning up!")
  }

  object Job {

    def apply(name: String): Behavior[Command] =
      Behaviors
        .receiveMessage[Command] { case ShutdownJob =>
          Behaviors.stopped
        }
        .receiveSignal { case (context, PostStop) =>
          context.log.info("job post stop")
          Behaviors.same
        }
  }

}
