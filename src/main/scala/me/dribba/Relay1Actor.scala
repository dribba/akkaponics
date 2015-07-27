package me.dribba

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.after
import scala.concurrent.Future
import scala.concurrent.duration._
import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.GpioPinDigitalOutput
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.RaspiPin

class Relay1Actor extends Actor with ActorLogging {

  import Relay1Actor._
  implicit val _ = context.dispatcher

  val gpio = GpioFactory.getInstance()
  val pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "relay", PinState.LOW)

  def receive = {
    case On =>
      pin.toggle()

      after(1 seconds, context.system.scheduler)(Future.successful(self ! Off))

  	case m: RelayState =>
      pin.toggle()

      after(5 seconds, context.system.scheduler)(Future.successful(self ! m.toggle))
  }	
}

object Relay1Actor {
  val props = Props[Relay1Actor]

  trait RelayState {
    def toggle: RelayState
  }

  case object On extends RelayState {
    def toggle = Off
  }
  case object Off extends RelayState {
    def toggle = On
  }
}