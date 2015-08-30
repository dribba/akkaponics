package me.dribba.components

import com.pi4j.io.gpio.{PinState, GpioPinDigitalOutput}

class GrowBedComponentLike(pin: GpioPinDigitalOutput) extends GrowBedComponent {

  pin.setShutdownOptions(true, PinState.LOW)

  override def turnWaterOn(): Unit =
    pin.high()


  override def turnWaterOff(): Unit =
    pin.low()
}
