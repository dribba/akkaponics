package me.dribba

import me.dribba.models.aquaponics.AquaponicsSystem
import me.dribba.models.scheduler.AquaponicsScheduler
import org.scalatest.WordSpecLike
import org.scalatest.Matchers


class AquaponicsSchedulerSpec extends WordSpecLike with Matchers {

  val scheduler = new AquaponicsScheduler()

  "AquaponicsScheduler" should {

    "add an AquaponicsSystem to the schedule" in {
      val system = new AquaponicsSystem()

      system.schedule(scheduler)

    }
  }


}
