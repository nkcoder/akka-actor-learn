package demo.actor.introduction.chatroom.oo

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

object ChatRoom {

  def apply(): Behavior[RoomCommand] =
    Behaviors.setup(context => new ChatRoomBehavior(context))

  sealed trait RoomCommand

  sealed trait SessionEvent

  trait SessionCommand

  final case class GetSession(screenName: String, replyTo: ActorRef[SessionEvent])
      extends RoomCommand

  final case class SessionGranted(handle: ActorRef[PostMessage]) extends SessionEvent

  final case class SessionDenied(reason: String) extends SessionEvent

  final case class MessagePosted(screenName: String, message: String) extends SessionEvent

  final case class PostMessage(message: String) extends SessionCommand

  class ChatRoomBehavior(context: ActorContext[RoomCommand])
      extends AbstractBehavior[RoomCommand](context) {
    private var sessions: List[ActorRef[SessionCommand]] = List.empty

    override def onMessage(message: RoomCommand): Behavior[RoomCommand] = {
      message match {
        case GetSession(screenName, client) =>
          // create a child actor for further interaction with the client
          val ses = context.spawn(
            SessionBehavior(context.self, screenName, client),
            name = URLEncoder.encode(screenName, StandardCharsets.UTF_8.name)
          )
          client ! SessionGranted(ses)
          sessions = ses :: sessions
          this
        case PublishSessionMessage(screenName, message) =>
          val notification = NotifyClient(MessagePosted(screenName, message))
          sessions.foreach(_ ! notification)
          this
      }
    }
  }

  private final case class NotifyClient(message: MessagePosted) extends SessionCommand

  private final case class PublishSessionMessage(screenName: String, message: String)
      extends RoomCommand

  private class SessionBehavior(
      context: ActorContext[SessionCommand],
      room: ActorRef[PublishSessionMessage],
      screenName: String,
      client: ActorRef[SessionEvent]
  ) extends AbstractBehavior[SessionCommand](context) {

    override def onMessage(msg: SessionCommand): Behavior[SessionCommand] = {
      msg match {
        case PostMessage(message) =>
          // from client, publish to others via the room
          room ! PublishSessionMessage(screenName, message)
          Behaviors.same
        case NotifyClient(message) =>
          // published from the room
          client ! message
          Behaviors.same
      }
    }
  }

  object SessionBehavior {
    def apply(
        room: ActorRef[PublishSessionMessage],
        screenName: String,
        client: ActorRef[SessionEvent]
    ): Behavior[SessionCommand] =
      Behaviors.setup(ctx => new SessionBehavior(ctx, room, screenName, client))
  }

}
