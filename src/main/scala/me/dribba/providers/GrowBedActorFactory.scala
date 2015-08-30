package me.dribba.providers

import akka.actor.{ActorContext, ActorRef}
import me.dribba.models.aquaponics.GrowBed


trait GrowBedActorFactory {

  def create(context: ActorContext, supervisor: ActorRef, growBed: GrowBed): ActorRef

}
