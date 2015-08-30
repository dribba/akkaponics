package me.dribba.providers

import akka.actor.{Cancellable, ActorRef}
import me.dribba.TestTimeout
import me.dribba.providers.GrowBedTimeoutProvider

import scala.concurrent.ExecutionContext


class TestGrowBedTimeoutProvider() extends GrowBedTimeoutProvider {

  val sensor = TestTimeout()
  val flush = TestTimeout()

  override def sensorTimeout(implicit growBedActor: ActorRef, executionContext: ExecutionContext): Cancellable =
    sensor.cancellable

  override def flushTimeout(implicit growBedActor: ActorRef, executionContext: ExecutionContext): Cancellable =
    flush.cancellable

}
