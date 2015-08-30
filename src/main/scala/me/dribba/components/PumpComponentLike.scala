package me.dribba.components

import com.pi4j.io.gpio.{PinState, GpioPinDigitalOutput}

class PumpComponentLike(pin: GpioPinDigitalOutput) extends PumpComponent {

  // Always make sure the pump will turn off when the app dies
  pin.setShutdownOptions(true, PinState.LOW)

  override def off(): Unit = pin.low()


  override def on(): Unit = pin.high()
}
