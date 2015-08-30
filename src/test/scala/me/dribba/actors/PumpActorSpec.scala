package me.dribba.actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import me.dribba.components.PumpComponent
import me.dribba.models.Status
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class PumpActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("PumpActorSpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  var pump = new PumpComponent {

    override def on: Unit = ()

    override def off: Unit = ()
  }

  "A PumpActor" must {

    "turn the pump on when it's off" in {
      val pumpActor = system.actorOf(PumpActor.props(pump))

      pumpActor ! PumpStatus
      expectMsg(Status.Off)

      pumpActor ! TurnPumpOn
      pumpActor ! PumpStatus

      expectMsg(Status.On)
    }

    "turn the pump off when it's on" in {
      val pumpActor = system.actorOf(PumpActor.props(pump))

      pumpActor ! TurnPumpOn

      pumpActor ! PumpStatus
      expectMsg(Status.On)

      pumpActor ! TurnPumpOff
      pumpActor ! PumpStatus

      expectMsg(Status.Off)
    }
  }

}
