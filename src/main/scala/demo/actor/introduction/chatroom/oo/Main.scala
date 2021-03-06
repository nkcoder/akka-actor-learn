package demo.actor.introduction.chatroom.oo

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, Terminated}

object Main {
  def main(args: Array[String]): Unit = {
    ActorSystem(Main(), "ChatRoomDemo")
  }

  def apply(): Behavior[NotUsed] =
    Behaviors.setup { context =>
      val chatRoom   = context.spawn(ChatRoom(), "chatroom")
      val gabblerRef = context.spawn(Gabbler(), "gabbler")
      context.watch(gabblerRef)
      chatRoom ! ChatRoom.GetSession("ol’ Gabbler", gabblerRef)

      Behaviors.receiveSignal { case (_, Terminated(_)) =>
        Behaviors.stopped
      }
    }

}
