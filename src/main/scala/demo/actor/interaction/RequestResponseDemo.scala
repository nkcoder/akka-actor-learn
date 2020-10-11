package demo.actor.interaction

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

/**
  * Useful when:
  *
  * - Subscribing to an actor that will send many response messages back
  *
  * Problems:
  *
  * - Actors seldom have a response message from another actor as a part of their protocol (see adapted response)
  * - It is hard to detect that a message request was not delivered or processed (see ask)
  * - Unless the protocol already includes a way to provide context, for example a request id that is also sent in the
  * response, it is not possible to tie an interaction to some specific context without introducing a new, separate,
  * actor (see ask or per session child actor)
  */
object RequestResponseDemo {

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
          cookieFabric ! Request("ask for my cookies", context.self)
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
