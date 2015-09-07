package me.dribba

import akka.actor.ActorSystem
import akka.event.{LogSource, Logging}
import com.pi4j.io.gpio._
import com.typesafe.config.ConfigFactory
import me.dribba.actors.{AllAtOnceSupervisorActor, PumpActor}
import me.dribba.components.PumpComponentLike
import me.dribba.models.aquaponics.GrowBed
import me.dribba.providers.TimedGrowBedActorFactoryLike
import me.dribba.utils.DurationUtils._

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

    implicit val config = ConfigFactory.load()

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
      GrowBed("GrowBed1", gpioOutPin(GROW_BED1), gpioInPin(GROW_BED1_SENSOR), finiteFromConfig("akkaponics.growBeds.bed1.flushTime")),
      GrowBed("GrowBed2", gpioOutPin(GROW_BED2), gpioInPin(GROW_BED2_SENSOR), finiteFromConfig("akkaponics.growBeds.bed2.flushTime"))
    )

    val pumpComponent = new PumpComponentLike(gpioOutPin(PUMP, Some("PumpPin")))

    val system = ActorSystem("Akkaponics")

    val log = Logging(system, new AppLogger)

    log.info(config + "")

    val cycle = finiteFromConfig("akkaponics.totalFlushCycle")

    log.info("Starting application")

    val pump = system.actorOf(PumpActor.props(pumpComponent))

    log.info("Created pump")

    val growBedActorFactory = new TimedGrowBedActorFactoryLike(pump)

    log.info("Created grow bed factory. Starting supervisor...")

    system.actorOf(AllAtOnceSupervisorActor.props(applicationGrowBeds, cycle, growBedActorFactory, config), "GrowBedSupervisor")

    log.info("Started!")
  }

}

class AppLogger {
  override def toString: String = "AppLogger"
}
