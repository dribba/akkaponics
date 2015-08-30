package me.dribba.components

import me.dribba.models.Status

trait DigitalSensorComponent {

  def currentStatus: Status

  def listen(): Unit

  def stopListening(): Unit

}