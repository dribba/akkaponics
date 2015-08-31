package me.dribba.providers

import akka.actor.{Cancellable, ActorRef, Scheduler}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class GrowBedTimeoutProviderLike(scheduler: Scheduler) extends GrowBedTimeoutProvider {

  override def sensorTimeout(implicit growBedActor: ActorRef, executionContext: ExecutionContext): Cancellable =
    scheduler.scheduleOnce(2 minute, growBedActor, SensorTookTooLong)

  override def flushTimeout(implicit growBedActor: ActorRef, executionContext: ExecutionContext): Cancellable =
    scheduler.scheduleOnce(45 seconds, growBedActor, FlushingTookTooLong)

}
