package me.dribba.providers

import akka.actor.{ActorRef, Cancellable}

import scala.concurrent.ExecutionContext

trait GrowBedTimeoutProvider {

  def sensorTimeout(implicit growBedActor: ActorRef, executionContext: ExecutionContext): Cancellable

  def flushTimeout(implicit growBedActor: ActorRef, executionContext: ExecutionContext): Cancellable

}

sealed trait GrowBedTimeoutMessage

object SensorTookTooLong extends GrowBedTimeoutMessage
object FlushingTookTooLong extends GrowBedTimeoutMessage