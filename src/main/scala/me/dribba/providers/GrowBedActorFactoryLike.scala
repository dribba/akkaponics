package me.dribba.providers

import akka.actor.{ActorContext, ActorRef}
import com.pi4j.io.gpio.GpioPinDigitalInput
import me.dribba.actors.GrowBedActor
import me.dribba.components.{DigitalSensorComponentLike, GrowBedComponentLike}
import me.dribba.models.aquaponics.GrowBed


class GrowBedActorFactoryLike(pumpActor: ActorRef) extends GrowBedActorFactory {

  def flushSensorFactory(sensorPin: GpioPinDigitalInput) = (actor: ActorRef) =>
    new DigitalSensorComponentLike(sensorPin, actor)


  override def create(context: ActorContext, supervisor: ActorRef, growBed: GrowBed): ActorRef = {
    val sensorFactory = flushSensorFactory(growBed.sensorPin)
    val timeoutProvider = new GrowBedTimeoutProviderLike(context.system.scheduler)
    val growBedComponent = new GrowBedComponentLike(growBed.componentPin)

    context.actorOf(GrowBedActor.props(supervisor, pumpActor, growBedComponent, sensorFactory, timeoutProvider))
  }

}
