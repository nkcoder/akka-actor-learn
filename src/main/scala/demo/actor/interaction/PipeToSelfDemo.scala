package demo.actor.interaction

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import demo.actor.interaction.CustomerRepository.{
  Update,
  UpdateFailure,
  UpdateResult,
  UpdateSuccess
}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/** Useful when:
  *
  *   - Accessing APIs that are returning Future from an actor, such as a database or an external
  *     service
  *   - The actor needs to continue processing when the Future has completed
  *   - Keep context from the original request and use that when the Future has completed, for
  *     example an replyTo actor reference
  *
  * Problems:
  *
  *   - Boilerplate of adding wrapper messages for the results
  */
object PipeToSelfDemo {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem(CustomerService(), "pipe-to-self")
    actorSystem.terminate()
  }
}

trait CustomerDataAccess {
  def update(value: Customer): Future[Done]
}

final case class Customer(id: String, version: Long, name: String, address: String)

case class CustomerDataAccessImpl() extends CustomerDataAccess {
  override def update(value: Customer): Future[Done] = Future.successful(Done)
}

object CustomerService {
  def apply(): Behavior[UpdateResult] = {
    Behaviors.setup(context => {
      val updateCustomer     = Update(Customer("1", 2L, "kobe", "LA"), context.self)
      val customerRepository = context.spawn(CustomerRepository(CustomerDataAccessImpl()), "repo")
      customerRepository ! updateCustomer

      Behaviors.receiveMessage {
        case UpdateSuccess(id) =>
          context.log.info(s"update customer for id: ${id} success")
          Behaviors.same
        case UpdateFailure(id, reason) =>
          context.log.warn("update customer for id: {} failed, reason: {}", id, reason)
          Behaviors.stopped
      }
    })
  }

  sealed trait Command
}

object CustomerRepository {
  private val MaxOperationsInProgress = 10

  def apply(dataAccess: CustomerDataAccess): Behavior[Command] = {
    next(dataAccess, operationsInProgress = 0)
  }

  private def next(
      dataAccess: CustomerDataAccess,
      operationsInProgress: Int
  ): Behavior[Command] = {
    Behaviors.receive { (context, command) =>
      command match {
        case message @ Update(value, replyTo) =>
          context.log.info("receive update command: {}", message)
          if (operationsInProgress == MaxOperationsInProgress) {
            context.log.warn("request exceeds max operations.")
            replyTo ! UpdateFailure(
              value.id,
              s"Max $MaxOperationsInProgress concurrent operations supported"
            )
            Behaviors.same
          } else {
            context.log.info("try to update customer.")
            val futureResult = dataAccess.update(value)
            /* It could be tempting to just use onComplete on the Future, but that introduces the
            risk of accessing internal state of the actor that is not thread -safe from an
            external thread.context
             */
            context.pipeToSelf(futureResult) {
              // map the Future value to a message, handled by this actor
              case Success(_) => WrappedUpdateResult(UpdateSuccess(value.id), replyTo)
              case Failure(e) =>
                WrappedUpdateResult(UpdateFailure(value.id, e.getMessage), replyTo)
            }
            // increase operationsInProgress counter
            next(dataAccess, operationsInProgress + 1)
          }

        case WrappedUpdateResult(result, replyTo) =>
          // send result to original requestor
          context.log.info("got update result: {}", result)
          replyTo ! result
          // decrease operationsInProgress counter
          next(dataAccess, operationsInProgress - 1)
      }
    }
  }

  sealed trait Command

  sealed trait UpdateResult

  final case class Update(value: Customer, replyTo: ActorRef[UpdateResult]) extends Command

  final case class UpdateSuccess(id: String) extends UpdateResult

  final case class UpdateFailure(id: String, reason: String) extends UpdateResult

  private final case class WrappedUpdateResult(
      result: UpdateResult,
      replyTo: ActorRef[UpdateResult]
  ) extends Command
}
