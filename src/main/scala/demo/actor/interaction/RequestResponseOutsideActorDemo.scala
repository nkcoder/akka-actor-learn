package demo.actor.interaction

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/** Useful when:
  *
  *   - Querying an actor from outside of the actor system
  *
  * Problems:
  *
  *   - It is easy to accidentally close over and unsafely mutable state with the callbacks on the
  *     returned Future as those will be executed on a different thread
  *   - There can only be a single response to one ask (see per session child Actor) When ask times
  *     out, the receiving actor does not know and may still process it to completion, or even start
  *     processing it after the fact
  */
object RequestResponseOutsideActorDemo {

  def main(args: Array[String]): Unit = {
    import akka.util.Timeout

    // asking someone requires a timeout if the timeout hits without response
    // the ask is failed with a TimeoutException
    implicit val timeout: Timeout = 3.seconds
    // implicit ActorSystem in scope
    implicit val system: ActorSystem[CookieFabric.GiveMeCookies] =
      ActorSystem(CookieFabric(), "request-response-outside-actor")

    // the response callback will be executed on this execution context
    implicit val ec: ExecutionContextExecutor = system.executionContext

    // 1. handle the InvalidRequest reply
    val validResult: Future[CookieFabric.Reply] =
      system.ask(ref => CookieFabric.GiveMeCookies(3, ref))

    validResult.onComplete {
      case Success(CookieFabric.Cookies(count)) =>
        println(s"Yay, $count cookies!")
      case Success(CookieFabric.InvalidRequest(reason)) =>
        println(s"No cookies for me. $reason")
      case Failure(ex) => println(s"Boo! didn't get cookies: ${ex.getMessage}")
    }

    // 2. map on the requestor side
    val invalidResult: Future[CookieFabric.Cookies] = system
      .ask(ref =>
        CookieFabric
          .GiveMeCookies(6, ref)
      )
      .flatMap {
        case c: CookieFabric.Cookies => Future.successful(c)
        case CookieFabric.InvalidRequest(reason) =>
          Future.failed(new IllegalArgumentException(reason))
      }

    invalidResult onComplete {
      case Success(CookieFabric.Cookies(count)) =>
        println(s"Yay, $count cookies")
      case Failure(ex) => println(s"Boo! didn't get cookies: ${ex.getMessage}")
    }

    system.terminate()

  }

  object CookieFabric {
    def apply(): Behaviors.Receive[GiveMeCookies] =
      Behaviors.receiveMessage { message =>
        if (message.count >= 5)
          message.replyTo ! InvalidRequest("Too many cookies.")
        else
          message.replyTo ! Cookies(message.count)
        Behaviors.same
      }

    sealed trait Command

    sealed trait Reply

    case class GiveMeCookies(count: Int, replyTo: ActorRef[Reply]) extends Command

    case class Cookies(count: Int) extends Reply

    case class InvalidRequest(reason: String) extends Reply
  }

}
