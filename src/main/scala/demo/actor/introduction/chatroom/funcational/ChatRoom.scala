package demo.actor.introduction.chatroom.funcational

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object ChatRoom {

  def apply(): Behavior[RoomCommand] =
    chatRoom(List.empty)

  /** functional style to create actor.
    *
    * @param sessions
    *   sessions for connected clients
    * @return
    *   behavior
    */
  private def chatRoom(sessions: List[ActorRef[SessionCommand]]): Behavior[RoomCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case GetSession(screenName, client) =>
          // create a child actor for further interaction with the client
          val ses = context.spawn(
            session(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name)
          )
          client ! SessionGranted(ses)
          chatRoom(ses :: sessions)
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }

  /** the session between the room and the client.
    *
    * @param room
    *   room actor
    * @param screenName
    *   client name
    * @param client
    *   client actor
    * @return
    *   behavior
    */
  private def session(
      room: ActorRef[PublishSessionMessage],
      screenName: String,
      client: ActorRef[SessionEvent]
  ): Behavior[SessionCommand] =
    Behaviors.receiveMessage {
      case PostMessage(message) =>
        // from client, publish to others via the room
        room ! PublishSessionMessage(screenName, message)
        Behaviors.same
      case NotifyClient(message) =>
        // published from the room
        client ! message
        Behaviors.same
    }

  sealed trait RoomCommand

  sealed trait SessionEvent

  trait SessionCommand

  final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent])
      extends RoomCommand

  final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent

  final case class SessionDenied(reason: String) extends SessionEvent

  final case class MessagePosted(screenName: String, message: String) extends SessionEvent

  final case class PostMessage(message: String) extends SessionCommand

  private final case class PublishSessionMessage(screenName: String, message: String)
      extends RoomCommand

  private final case class NotifyClient(message: MessagePosted) extends SessionCommand

}
