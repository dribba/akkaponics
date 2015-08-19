package me.dribba.actors

import akka.actor.Actor
import me.dribba.components.PumpComponent
import me.dribba.models.Status

class PumpActor(pump: PumpComponent) extends Actor {

  var status = Status.Off



  def receive = {
    case
  }

}

trait PumpMessage
object TurnPumpOn extends PumpMessage
object TurnPumpOff extends PumpMessage
