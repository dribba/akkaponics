package me.dribba.components

import me.dribba.models.Status


class TestGrowBedComponent(private var _status: Status = Status.Off) extends GrowBedComponent {

  def status = _status

  override def turnWaterOn(): Unit = _status = Status.On

  override def turnWaterOff(): Unit = _status = Status.Off
}
