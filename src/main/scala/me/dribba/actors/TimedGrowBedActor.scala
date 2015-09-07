package me.dribba.actors

import akka.actor._
import me.dribba.actors.TimedGrowBedActor.Flushing
import me.dribba.components.{DigitalSensorComponent, GrowBedComponent}
import me.dribba.models.aquaponics.GrowBed
import me.dribba.models.{Status, DigitalSensorStatus}
import me.dribba.providers.{FlushingTookTooLong, SensorTookTooLong, GrowBedTimeoutMessage, GrowBedTimeoutProvider}

class TimedGrowBedActor(
  supervisor: ActorRef,
  pumpActor: ActorRef,
  growBedComponent: GrowBedComponent,
  growBed: GrowBed
) extends Actor with ActorLogging {
  import context._

  override def preStart() = {
    growBedComponent.turnWaterOff() // JIC it hung up trying to flush
  }

  def receive = {
    case m: GrowBedMessage => nonFlushingGrowBedMessages(m)
  }

  def flushing: Receive = {
    case m: GrowBedMessage =>
      flushingGrowBedMessages(m)

    case Flushing =>
      growBedComponent.turnWaterOff()
      pumpActor ! TurnPumpOff
      log.info("Finished flushing in bed {}", self.path.toString)
      become(receive)

  }

  val nonFlushingGrowBedMessages: PartialFunction[GrowBedMessage, Unit] = {
    case Flush =>
      log.info("Started flushing in bed {}", self.path.toString)
      pumpActor ! TurnPumpOn
      growBedComponent.turnWaterOn()
      system.scheduler.scheduleOnce(growBed.flushTime, self, Flushing)
      become(flushing)
  }


  val flushingGrowBedMessages: PartialFunction[GrowBedMessage, Unit] = {
    case Flush =>
      log.info("Got Flush message when already flushing, from: {}", sender().path.toString)
      sender() ! AlreadyFlushing
  }


}

object TimedGrowBedActor {

  def props(supervisor: ActorRef,
            pumpActor: ActorRef,
            growBedComponent: GrowBedComponent,
            growBed: GrowBed) =
    Props(classOf[TimedGrowBedActor], supervisor, pumpActor, growBedComponent, growBed)

  private case object Flushing
}