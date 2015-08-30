package me.dribba.components

import akka.actor.ActorRef
import me.dribba.models.{DigitalSensorStatus, Status}


class TestDigitalSensorComponent(actor: ActorRef, private var status: Status = Status.Off) extends DigitalSensorComponent {

  override def currentStatus: Status = status

  def switchOn(): Unit = {
    status = Status.On
    actor ! DigitalSensorStatus(status)
  }

  def switchOff(): Unit = {
    status = Status.Off
    actor ! DigitalSensorStatus(status)
  }

  override def listen(): Unit = ???

  override def stopListening(): Unit = ???
}
