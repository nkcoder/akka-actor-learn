package demo.actor.interaction

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/** Useful when:
  *
  *   - Single response queries
  *   - An actor needs to know that the message was processed before continuing
  *   - To allow an actor to resend if a timely response is not produced
  *   - To keep track of outstanding requests and not overwhelm a recipient with messages
  *     (“backpressure”)
  *   - Context should be attached to the interaction but the protocol does not support that
  *     (request id, what query the response was for)
  *
  * Problems:
  *
  *   - There can only be a single response to one ask (see per session child Actor)
  *   - When ask times out, the receiving actor does not know and may still process it to
  *     completion, or even start processing it after the fact
  *   - Finding a good value for the timeout, especially when ask triggers chained asks in the
  *     receiving actor. You want a short timeout to be responsive and answer back to the requester,
  *     but at the same time you do not want to have many false positives
  */
object AskBetweenActorsDemo {

  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(Dave(), "ask-between-actors")
    actorSystem.terminate()
    actorSystem.whenTerminated.value
  }

  object Hal {

    def apply(): Behaviors.Receive[Command] =
      Behaviors.receiveMessage[Command] { case OpenThePodBayDoorsPlease(replyTo) =>
        replyTo ! Response("I'm sorry, Dave. I'm afraid I can't do that.")
        Behaviors.same
      }

    sealed trait Command

    case class OpenThePodBayDoorsPlease(replyTo: ActorRef[Response]) extends Command

    case class Response(message: String)
  }

  object Dave {

    def apply(): Behavior[Command] =
      Behaviors.setup[Command] { context =>
        // asking someone requires a timeout, if the timeout hits without response
        // the ask is failed with a TimeoutException

        implicit val timeout: Timeout = 3.seconds

        val hal = context.spawn(Hal(), "hal")

        // Note: The second parameter list takes a function `ActorRef[T] => Message`,
        // as OpenThePodBayDoorsPlease is a case class it has a factory apply method
        // that is what we are passing as the second parameter here it could also be written
        // as `ref => OpenThePodBayDoorsPlease(ref)`
        // context.ask(hal, ref => Hal.OpenThePodBayDoorsPlease(ref)) {
        context.ask(hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(Hal.Response(message)) => AdaptedResponse(message)
          case Failure(_)                     => AdaptedResponse("Request failed")
        }

        // we can also tie in request context into an interaction, it is safe to look at
        // actor internal state from the transformation function, but remember that it may have
        // changed at the time the response arrives and the transformation is done, best is to
        // use immutable state we have closed over like here.
        val requestId = 1
        context.ask(hal, Hal.OpenThePodBayDoorsPlease) {
          case Success(Hal.Response(message)) =>
            AdaptedResponse(s"$requestId: $message")
          case Failure(_) => AdaptedResponse(s"$requestId: Request failed")
        }

        Behaviors.receiveMessage {
          // the adapted message ends up being processed like any other
          // message sent to the actor
          case AdaptedResponse(message) =>
            context.log.info("Got response from hal: {}", message)
            Behaviors.same
        }
      }

    sealed trait Command

    // this is a part of the protocol that is internal to the actor itself
    private case class AdaptedResponse(message: String) extends Command
  }

}
