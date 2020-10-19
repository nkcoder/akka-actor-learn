package demo.actor.actordiscovery

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

/** When an actor needs to be discovered by another actor but you are unable to put a reference to
  * it in an incoming message, you can use the Receptionist. It supports both local and cluster(see
  * cluster). You register the specific actors that should be discoverable from each node in the
  * local Receptionist instance. The API of the receptionist is also based on actor messages. This
  * registry of actor references is then automatically distributed to all other nodes in the case of
  * a cluster. You can lookup such actors with the key that was used when they were registered. The
  * reply to such a Find request is a Listing, which contains a Set of actor references that are
  * registered for the key. Note that several actors can be registered to the same key.
  *
  * The registry is dynamic. New actors can be registered during the lifecycle of the system.
  * Entries are removed when registered actors are stopped, manually deregistered or the node they
  * live on is removed from the Cluster. To facilitate this dynamic aspect you can also subscribe to
  * changes with the Receptionist.Subscribe message. It will send Listing messages to the
  * subscriber, first with the set of entries upon subscription, then whenever the entries for a key
  * are changed.
  */
object ReceptionistDemo {

  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](Guardian(), "receptionist-demo")
  }

  object PingService {

    val pingServiceKey: ServiceKey[Ping] = ServiceKey[Ping]("pingService")

    def apply(): Behavior[Ping] = {
      Behaviors.setup { context =>
        context.system.receptionist ! Receptionist.Register(pingServiceKey, context.self)

        // If a server no longer wish to be associated with a service key it can deregister using
        // the command Receptionist.Deregister which will remove the association and inform all
        // subscribers.
//        context.system.receptionist ! Receptionist.Deregister(
//          PingService.pingServiceKey,
//          context.self
//        )

        Behaviors.receiveMessage { case Ping(replyTo) =>
          context.log.info("Pinged by {}", replyTo)
          replyTo ! Pong
          Behaviors.same
        }
      }
    }

    final case class Ping(replyTo: ActorRef[Pong.type])

    case object Pong
  }

  object Pinger {

    def apply(pingService: ActorRef[PingService.Ping]): Behavior[PingService.Pong.type] = {
      Behaviors.setup { context =>
        pingService ! PingService.Ping(context.self)

        Behaviors.receiveMessage { _ =>
          context.log.info("{} was ponged!!", context.self)
          Behaviors.same
        }
      }
    }
  }

  object Guardian {
    def apply(): Behavior[Nothing] = {
      Behaviors
        .setup[Receptionist.Listing] { context =>
          context.spawnAnonymous(PingService())
          // Subscribing means that the guardian actor will be informed of any new registrations
          // via a Listing message:
          context.system.receptionist ! Receptionist.Subscribe(
            PingService.pingServiceKey,
            context.self
          )
          context.spawnAnonymous(PingService())

          Behaviors.receiveMessagePartial[Receptionist.Listing] {
            case PingService.pingServiceKey.Listing(listings) =>
              context.log.info("listings: {}", listings)
              listings.foreach(ps => context.spawnAnonymous(Pinger(ps)))
              Behaviors.same
          }
        }
        .narrow
    }
  }

}
