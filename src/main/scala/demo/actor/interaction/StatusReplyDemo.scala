package demo.actor.interaction

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.pattern.StatusReply

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

/** To help with this a generic status-response type is included in Akka: StatusReply, everywhere
  * where ask can be used there is also a second method askWithStatus which, given that the response
  * is a StatusReply will unwrap successful responses and help with handling validation errors.
  */
object StatusReplyDemo {

  def main(args: Array[String]): Unit = {
    import akka.util.Timeout

    // asking someone requires a timeout if the timeout hits without response
    // the ask is failed with a TimeoutException
    implicit val timeout: Timeout = 3.microseconds
    implicit val system: ActorSystem[CookieFabric.GiveMeCookies] =
      ActorSystem(CookieFabric(), "status-reply")

    // the response callback will be executed on this execution context
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val validResult: Future[CookieFabric.Cookies] =
      system.askWithStatus(ref => CookieFabric.GiveMeCookies(3, ref))

    // A StatusReply.Success(m) ends up as a Success(m) here, while a
    // StatusReply.Error(text) becomes a Failure(ErrorMessage(text))
    validResult.onComplete {
      case Success(CookieFabric.Cookies(count)) =>
        println(s"Yay, $count cookies!")
      case Failure(StatusReply.ErrorMessage(reason)) =>
        println(s"No cookies for me. $reason")
      case Failure(ex) => println(s"Boo! didn't get cookies: ${ex.getMessage}")
    }

    system.terminate()

  }

  object CookieFabric {
    def apply(): Behaviors.Receive[GiveMeCookies] =
      Behaviors.receiveMessage { message =>
        if (message.count >= 5)
          // sleep to get timeout exception
          // TimeUnit.SECONDS.sleep(1)
          message.replyTo ! StatusReply.error("Too many cookies.")
        else {
          // sleep to get timeout exception
          // TimeUnit.SECONDS.sleep(1)
          message.replyTo ! StatusReply.success(Cookies(message.count))
        }
        Behaviors.same
      }

    sealed trait Command

    sealed trait Reply

    case class GiveMeCookies(count: Int, replyTo: ActorRef[StatusReply[Cookies]]) extends Command

    case class Cookies(count: Int) extends Reply

  }

}
