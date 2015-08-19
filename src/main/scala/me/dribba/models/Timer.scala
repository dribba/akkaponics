package me.dribba.models

import me.dribba.models.scheduler.ScheduledEvent

import scala.concurrent.duration.Duration


trait Timer {

  def schedule[A](evt: Event[A], duration: Duration): ScheduledEvent[A]

}
