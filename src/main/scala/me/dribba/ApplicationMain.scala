package me.dribba

import akka.actor.ActorSystem

object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val pingActor = system.actorOf(Relay1Actor.props, "relay")
  pingActor ! Relay1Actor.On

  system.awaitTermination()
}