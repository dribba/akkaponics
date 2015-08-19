package me.dribba.models.scheduler

import akka.actor.ActorRef

case class ScheduledEvent[A](actor: ActorRef, message: A)
