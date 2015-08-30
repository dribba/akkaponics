package me.dribba.components

import akka.actor.ActorRef
import com.pi4j.io.gpio.{PinState, GpioPinDigitalInput}
import com.pi4j.io.gpio.event.{GpioPinDigitalStateChangeEvent, GpioPinListenerDigital}
import me.dribba.models.{DigitalSensorStatus, Status}


class DigitalSensorComponentLike(sensor: GpioPinDigitalInput, actor: ActorRef) extends DigitalSensorComponent {


  val listener = new GpioPinListenerDigital {
    override def handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent): Unit = {
      val state = event.getState match {
        case PinState.HIGH => Status.On
        case PinState.LOW => Status.Off
      }

      actor ! DigitalSensorStatus(state)
    }
  }


  sensor.addListener(listener)

}
