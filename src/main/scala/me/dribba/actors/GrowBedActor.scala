package me.dribba.actors

import akka.actor._
import me.dribba.components.{DigitalSensorComponent, GrowBedComponent}
import me.dribba.models.{Status, DigitalSensorStatus}
import me.dribba.providers.{FlushingTookTooLong, SensorTookTooLong, GrowBedTimeoutMessage, GrowBedTimeoutProvider}

class GrowBedActor(
  supervisor: ActorRef,
  pumpActor: ActorRef,
  growBed: GrowBedComponent,
  flushSensorFactory: (ActorRef) => DigitalSensorComponent,
  timeoutProvider: GrowBedTimeoutProvider
) extends Actor with ActorLogging {
  import context._

  override def preStart() = {
    growBed.turnWaterOff() // JIC it hung up trying to flush
    flushSensor = flushSensorFactory(self)
  }

  var flushSensor: DigitalSensorComponent = null

  def outOfService: Receive = {
    case _ => sender() ! OutOfService
  }

  def receive = {
    case m: GrowBedMessage => nonFlushingGrowBedMessages(m)
    case m: DigitalSensorStatus =>
      log.info("Got sensor status({}) while not flushing", m)
  }

  def flushing(timeout: Cancellable): Receive = {
    case m: GrowBedMessage =>
      flushingGrowBedMessages(m)

    case m: GrowBedTimeoutMessage => m match {
      case SensorTookTooLong | FlushingTookTooLong =>
        // SensorTookTooLong: Water took too long to reach the sensor.
        // FlushingTookTooLong: Water is not flushing form the grow bed or not flushing fast enough.
        growBed.turnWaterOff()
        pumpActor ! TurnPumpOff
        log.info("Bed {} is going OOS because of {}", self.path.toString, m)
        supervisor ! OutOfService
        become(outOfService)
    }

    case DigitalSensorStatus(status) =>
      status match {
        case Status.On =>
          // Level of water reached the sensor
          timeout.cancel()
          become(flushing(timeoutProvider.flushTimeout))

        case Status.Off =>
          // Bed is flushing
          log.info("Sensor triggered")
          timeout.cancel()
          growBed.turnWaterOff()
          pumpActor ! TurnPumpOff
          log.info("Finished flushing in bed {}", self.path.toString)
          become(receive)
      }
  }

  val nonFlushingGrowBedMessages: PartialFunction[GrowBedMessage, Unit] = {
    case Flush =>
      log.info("Started flushing in bed {}", self.path.toString)
      pumpActor ! TurnPumpOn
      growBed.turnWaterOn()
      become(flushing(timeoutProvider.sensorTimeout))
  }


  val flushingGrowBedMessages: PartialFunction[GrowBedMessage, Unit] = {
    case Flush =>
      log.warning("Got Flush message when already flushing, from: {}", sender().path.toString)
      sender() ! AlreadyFlushing
  }


}

object GrowBedActor {

  def props(supervisor: ActorRef, pumpActor: ActorRef, growBed: GrowBedComponent,
            flushSensorFactory: (ActorRef) => DigitalSensorComponent,
            timeoutProvider: GrowBedTimeoutProvider) =
    Props(classOf[GrowBedActor], supervisor, pumpActor, growBed, flushSensorFactory, timeoutProvider)
}


sealed trait GrowBedMessage
object Flush extends GrowBedMessage

object AlreadyFlushing extends GrowBedMessage
object OutOfService extends GrowBedMessage
