package me.dribba.components

import akka.actor.ActorRef
import me.dribba.models.{DigitalSensorStatus, Status}


class TestDigitalSensorComponent(actor: ActorRef, private var status: Status = Status.Off) extends DigitalSensorComponent {

  def switchOn(): Unit = {
    status = Status.On
    actor ! DigitalSensorStatus(status)
  }

  def switchOff(): Unit = {
    status = Status.Off
    actor ! DigitalSensorStatus(status)
  }

}
