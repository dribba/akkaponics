package me.dribba.models

import akka.actor.{ActorRefFactory, ActorRef}
import me.dribba.models.aquaponics.GrowBed


trait GrowBedActorFactory {

  def create(factory: ActorRefFactory, supervisor: ActorRef, growBed: GrowBed): ActorRef

}
