package me.dribba.actors

import akka.actor.{Props, ActorLogging, Actor}
import me.dribba.components.PumpComponent
import me.dribba.models.Status
import me.dribba.models.Status._

class PumpActor(pump: PumpComponent) extends Actor with ActorLogging {

  private var status: Status = Off

  override def preStart() = {
    pump.off
  }

  private val pumpMessage: PartialFunction[PumpMessage, Unit] = {
    case TurnPumpOff =>
        pump.off
        status = Off

    case TurnPumpOn =>
        pump.on
        status = On

    case PumpStatus =>
      sender() ! status
  }


  def receive: Receive = {
    case m: PumpMessage => pumpMessage(m)
  }

}

object PumpActor {

  def props(pump: PumpComponent) = Props(classOf[PumpActor], pump)

}

sealed trait PumpMessage
object TurnPumpOn extends PumpMessage
object TurnPumpOff extends PumpMessage
object PumpStatus extends PumpMessage

