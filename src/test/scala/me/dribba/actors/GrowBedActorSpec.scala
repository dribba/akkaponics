package me.dribba.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import me.dribba.components.{TestGrowBedComponent, GrowBedComponent, TestDigitalSensorComponent}
import me.dribba.models.{DigitalSensorStatus, Status}
import me.dribba.providers.TestGrowBedTimeoutProvider
import me.dribba.providers.{FlushingTookTooLong, SensorTookTooLong, GrowBedTimeoutProvider}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class GrowBedActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("PumpActorSpec"))

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  val fakeSensor = (actor: ActorRef) => new TestDigitalSensorComponent(actor)

  def fakeTimeoutProvider = new TestGrowBedTimeoutProvider()

  def fakeComponent = new TestGrowBedComponent()

  def props(supervisor: ActorRef, pumpActor: ActorRef, growBedComponent: GrowBedComponent, timeoutProvider: GrowBedTimeoutProvider) =
    GrowBedActor.props(supervisor, pumpActor, growBedComponent, fakeSensor, timeoutProvider)

  def simpleProps =
    GrowBedActor.props(TestProbe().ref, TestProbe().ref, fakeComponent, fakeSensor, fakeTimeoutProvider)

  "A GrowBedActor" must {


    "respond with AlreadyFlushing when is already flushing" in {
      val growBedActor = system.actorOf(simpleProps)

      growBedActor ! Flush
      growBedActor ! Flush

      expectMsg(AlreadyFlushing)
    }

    "do a full flush" in {
      val pumpActor = TestProbe()
      val component = fakeComponent
      val timeoutProvider = fakeTimeoutProvider
      val growBedActor = system.actorOf(props(TestProbe().ref, pumpActor.ref, component, timeoutProvider))

      growBedActor ! Flush
      expectNoMsg()

      // Will ask the pump to turn on
      pumpActor.expectMsg(TurnPumpOn)

      // Then turn on the component
      component.status shouldBe Status.On

      // Timeouts should not be cancelled yet
      timeoutProvider.sensor.cancellable.isCancelled shouldBe false
      timeoutProvider.flush.cancellable.isCancelled shouldBe false

      // The water reached the sensor and started flushing
      growBedActor ! DigitalSensorStatus(Status.On)

      // This test sensor "flickering", need to make sure bed wasn't shut
      growBedActor ! DigitalSensorStatus(Status.Off)
      // To make sure bed still flushing, ask to flush again
      growBedActor ! Flush
      expectMsg(AlreadyFlushing)
      expectNoMsg() // This will make it wait enough to avoid the sensor flickering status

      // Now trigger a flush
      growBedActor ! DigitalSensorStatus(Status.Off)

      // Should cancel the sensor timeout
      timeoutProvider.sensor.cancellable.isCancelled shouldBe true

      growBedActor ! DigitalSensorStatus(Status.Off)
      expectNoMsg()

      // Cancel the flush timeout
      timeoutProvider.flush.cancellable.isCancelled shouldBe true

      // Then it should turn the water off
      component.status shouldBe Status.Off

      // and ask the pump to be turned off
      pumpActor.expectMsg(TurnPumpOff)

      // Make sure it isn't still in flushing state
      growBedActor ! Flush
      expectNoMsg()
    }

    "tell it's parent when the bed goes OOS because of sensor timeout" in {
      val pumpActor = TestProbe()
      val supervisor = TestProbe()
      val component = fakeComponent
      val timeoutProvider = fakeTimeoutProvider
      val growBedActor = system.actorOf(props(supervisor.ref, pumpActor.ref, component, timeoutProvider))

      growBedActor ! Flush
      expectNoMsg()

      // Will ask the pump to turn on
      pumpActor.expectMsg(TurnPumpOn)

      // Then turn on the component
      component.status shouldBe Status.On

      // Timeouts should not be cancelled yet
      timeoutProvider.sensor.cancellable.isCancelled shouldBe false
      timeoutProvider.flush.cancellable.isCancelled shouldBe false

      // The water took too long to reach the sensor
      growBedActor ! SensorTookTooLong
      // And tell pump to turn off
      pumpActor.expectMsg(TurnPumpOff)

      // Tell the parent that the bed is OOS
      supervisor.expectMsg(OutOfService)

      // Should cancel the sensor timeout
      timeoutProvider.sensor.cancellable.isCancelled shouldBe false
      // Should turn water off
      component.status shouldBe Status.Off

      // Make sure bed is OOS
      growBedActor ! Flush
      expectMsg(OutOfService)
    }
    "tell it's parent when the bed goes OOS because of flush timeout" in {
      val pumpActor = TestProbe()
      val supervisor = TestProbe()
      val component = fakeComponent
      val timeoutProvider = fakeTimeoutProvider
      val growBedActor = system.actorOf(props(supervisor.ref, pumpActor.ref, component, timeoutProvider))

      growBedActor ! Flush
      expectNoMsg()

      // Will ask the pump to turn on
      pumpActor.expectMsg(TurnPumpOn)

      // Then turn on the component
      component.status shouldBe Status.On

      // Timeouts should not be cancelled yet
      timeoutProvider.sensor.cancellable.isCancelled shouldBe false
      timeoutProvider.flush.cancellable.isCancelled shouldBe false

      // The water reached the sensor and started flushing
      growBedActor ! DigitalSensorStatus(Status.On)
      expectNoMsg()

      // Should cancel the sensor timeout
      timeoutProvider.sensor.cancellable.isCancelled shouldBe true

      // The flush timeout was fired
      growBedActor ! FlushingTookTooLong

      // Shut the pump
      pumpActor.expectMsg(TurnPumpOff)

      // Tell the parent that the bed is OOS
      supervisor.expectMsg(OutOfService)

      // Don't say anything to the sender
      expectNoMsg()

      // Cancel timeout was never canceled
      timeoutProvider.flush.cancellable.isCancelled shouldBe false

      // Then it should turn the water off
      component.status shouldBe Status.Off

      // Make sure bed is OOS
      growBedActor ! Flush
      expectMsg(OutOfService)
    }

    "shut the component if it's on when starting" in {
      val pumpActor = TestProbe()
      val supervisor = TestProbe()
      val component = new TestGrowBedComponent(Status.On)
      val timeoutProvider = fakeTimeoutProvider

      // Start the actor
      system.actorOf(props(supervisor.ref, pumpActor.ref, component, timeoutProvider))

      expectNoMsg() // Just wait for the actor to start(Ugly but hey "have to make async => sync")

      component.status shouldBe Status.Off
    }


  }

}
