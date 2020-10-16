package demo.actor.interaction

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

/** Useful when:
  *
  *   - Sending a message for which the protocol defines a reply, but you are not interested in
  *     getting the reply
  *
  * Problems:
  *
  * The returned ActorRef ignores all messages sent to it, therefore it should be used carefully.
  *
  *   - Passing it around inadvertently as if it was a normal ActorRef may result in broken
  *     actor-to-actor interactions.
  *   - Using it when performing an ask from outside the Actor System will cause the Future returned
  *     by the ask to timeout since it will never complete.
  *   - Finally, it’s legal to watch it, but since it’s of a special kind, it never terminates and
  *     therefore you will never receive a Terminated signal from it.
  */
object IgnoreRefActor {
  def main(args: Array[String]): Unit = {
    ActorSystem(Requestor(), "request-response")
  }

  object CookieFabric {
    import Requestor.Response

    def apply(): Behavior[Request] = {
      Behaviors.receive { case (context, message @ Request(query, replyTo)) =>
        context.log.info(s"receive message: $message")
        replyTo ! Response(s"Here are the cookies for: [$query]")
        Behaviors.same
      }
    }

    sealed trait Command

    case class Request(query: String, replyTo: ActorRef[Response]) extends Command

  }

  object Requestor {

    import CookieFabric.Request

    def apply(): Behavior[Response] = {
      Behaviors.setup[Response] { context =>
        {
          val cookieFabric = context.spawn(CookieFabric(), "cookie-fabric")
          // context.system.ignoreRef: return ActorRef that ignores all messages
          cookieFabric ! Request("ask for my cookies", context.system.ignoreRef)
          // will never receive the Response message
          Behaviors.receiveMessage[Response] { message =>
            context.log.info("receive message: {} ", message)
            Behaviors.stopped
          }
        }
      }
    }

    sealed trait Command

    case class Response(result: String) extends Command

  }

}
