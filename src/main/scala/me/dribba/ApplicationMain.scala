package me.dribba

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import com.pi4j.io.gpio._
import me.dribba.actors.{GrowBedSupervisorActor, PumpActor}
import me.dribba.components.PumpComponentLike
import me.dribba.models.aquaponics.GrowBed
import me.dribba.providers.GrowBedActorFactoryLike

import scala.concurrent.duration._

object ApplicationMain extends App {


  object ApplicationPins {
    val PUMP = RaspiPin.GPIO_21
    val GROW_BED1 = RaspiPin.GPIO_22
    val GROW_BED2 = RaspiPin.GPIO_23
    val GROW_BED1_SENSOR = RaspiPin.GPIO_28
    val GROW_BED2_SENSOR = RaspiPin.GPIO_29
  }

  override def main(args: Array[String]) = {

    import ApplicationPins._

    implicit val myLogSourceType: LogSource[AppLogger] = new LogSource[AppLogger] {
      def genString(a: AppLogger) = "AppLogger"
      override def genString(a: AppLogger, s: ActorSystem) = "AppLogger ," + s
    }

    val gpio = GpioFactory.getInstance()

    def gpioOutPin(pin: Pin, nameOpt: Option[String] = None): GpioPinDigitalOutput =
      nameOpt match {
        case Some(name) => gpio.provisionDigitalOutputPin(pin, name, PinState.LOW)
        case None  => gpio.provisionDigitalOutputPin(pin, PinState.LOW)
      }

    def gpioInPin(pin: Pin, nameOpt: Option[String] = None): GpioPinDigitalInput = {
      nameOpt match {
        case Some(name) => gpio.provisionDigitalInputPin(pin, name, PinPullResistance.PULL_DOWN)
        case None  => gpio.provisionDigitalInputPin(pin, PinPullResistance.PULL_DOWN)
      }
    }


    val applicationGrowBeds = List(
      GrowBed("GrowBed1", gpioOutPin(GROW_BED1), gpioInPin(GROW_BED1_SENSOR)),
      GrowBed("GrowBed2", gpioOutPin(GROW_BED2), gpioInPin(GROW_BED2_SENSOR))
    )



    val pumpComponent = new PumpComponentLike(gpioOutPin(PUMP, Some("PumpPin")))

    val system = ActorSystem("Akkaponics")

    val log = Logging(system, new AppLogger)

    log.info("Starting application")

    val pump = system.actorOf(PumpActor.props(pumpComponent))

    log.info("Created pump")

    val growBedActorFactory = new GrowBedActorFactoryLike(pump)

    log.info("Created grow bed factory. Starting supervisor...")

    val supervisor = system.actorOf(GrowBedSupervisorActor.props(applicationGrowBeds, 5 minutes, growBedActorFactory))

  }

}

class AppLogger {
  override def toString: String = "AppLogger"
}