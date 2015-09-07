package me.dribba.models.aquaponics

import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput}

import scala.concurrent.duration.FiniteDuration

case class GrowBed(name: String, componentPin: GpioPinDigitalOutput, sensorPin: GpioPinDigitalInput, flushTime: FiniteDuration)
