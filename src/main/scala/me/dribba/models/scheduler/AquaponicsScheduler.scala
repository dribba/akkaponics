package me.dribba.models.scheduler

import scala.concurrent.duration.Duration

trait AquaponicsScheduler {


  def schedule[A](task: Task[A], duration: Duration): TaskSubscription[A]

  def schedule[A](task: ScheduledEvent[A], duration: Duration): Subscription[A]


}
