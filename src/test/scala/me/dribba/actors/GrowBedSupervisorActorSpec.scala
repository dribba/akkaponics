package me.dribba.actors

import akka.actor.{Props, ActorRef, ActorRefFactory, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import com.pi4j.io.gpio.{GpioPinDigitalInput, GpioPinDigitalOutput}
import me.dribba.models.GrowBedActorFactory
import me.dribba.models.aquaponics.GrowBed
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class GrowBedSupervisorActorSpec (_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with MockitoSugar {

  def this() = this(ActorSystem("PumpActorSpec"))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }


  class UnscheduledSupervisorActor(
      beds: List[GrowBed],
      cycleEvery: FiniteDuration,
      growBedFactory: GrowBedActorFactory
    ) extends GrowBedSupervisorActor(beds, cycleEvery, growBedFactory)
  {
    override def scheduleNext(): Unit = {}
    override def start(): Unit = {}
  }

  def inputPinMock = mock[GpioPinDigitalInput]
  def outputPinMock = mock[GpioPinDigitalOutput]


  val growBed1 = GrowBed("GrowBed1", outputPinMock, inputPinMock)
  val growBed2 = GrowBed("GrowBed2", outputPinMock, inputPinMock)

  val defaultDuration = 10 minutes

  val sampleGrowBeds = List(growBed1, growBed2)

  def stubFactory(factory: GrowBed => ActorRef) = new GrowBedActorFactory {
    override def create(actorFactory: ActorRefFactory, supervisor: ActorRef, growBed: GrowBed): ActorRef =
      factory(growBed)
  }

  def simpleFactory = stubFactory(_ => TestProbe().ref)

  def supervisorProps(
                       growBeds: List[GrowBed] = sampleGrowBeds,
                       cycle: FiniteDuration = defaultDuration,
                       factory: GrowBedActorFactory = simpleFactory) =
    Props(new UnscheduledSupervisorActor(growBeds, cycle, factory))


  "A GrowBedSupervisorActorSpec" must {

    "create the growBeds on start" in {
      val supervisor = system.actorOf(supervisorProps())

      supervisor ! GetActiveBeds

      expectMsg(ActiveBeds(sampleGrowBeds))
    }

    "send Flush message to grow beds" in {
      val bed1Probe = TestProbe()
      val bed2Probe = TestProbe()

      val factory = stubFactory(bed =>
        if(bed == growBed1)
          bed1Probe.ref
        else
          bed2Probe.ref
      )

      val supervisor = system.actorOf(supervisorProps(factory = factory))

      supervisor ! FlushNext
      bed1Probe.expectMsg(Flush)

      supervisor ! FlushNext
      bed2Probe.expectMsg(Flush)

      // Make sure it keeps re-queuing
      supervisor ! FlushNext
      bed1Probe.expectMsg(Flush)
    }
    "remove a bed from active beds when it goes OOS" in {
      val bed1Probe = TestProbe()
      val bed2Probe = TestProbe()

      val factory = stubFactory(bed =>
        if(bed == growBed1)
          bed1Probe.ref
        else
          bed2Probe.ref
      )

      val supervisor = system.actorOf(supervisorProps(factory = factory))

      supervisor.tell(OutOfService, bed1Probe.ref)
      expectNoMsg()

      supervisor ! GetActiveBeds

      expectMsg(ActiveBeds(List(growBed2)))

      supervisor ! FlushNext
      bed2Probe.expectMsg(Flush)

      supervisor ! FlushNext
      bed2Probe.expectMsg(Flush)
    }
  }


}
