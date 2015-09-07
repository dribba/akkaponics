package me.dribba.providers

import akka.actor.{ActorContext, ActorRef}
import me.dribba.actors.TimedGrowBedActor
import me.dribba.components.GrowBedComponentLike
import me.dribba.models.aquaponics.GrowBed


class TimedGrowBedActorFactoryLike(pumpActor: ActorRef) extends GrowBedActorFactory {

  override def create(context: ActorContext, supervisor: ActorRef, growBed: GrowBed): ActorRef = {
    val growBedComponent = new GrowBedComponentLike(growBed.componentPin)

    context.actorOf(TimedGrowBedActor.props(supervisor, pumpActor, growBedComponent, growBed), growBed.name + "Actor")
  }

}
