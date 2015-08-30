package me.dribba.models.aquaponics

import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput}

case class GrowBed(name: String, componentPin: GpioPinDigitalOutput, sensorPin: GpioPinDigitalInput)
