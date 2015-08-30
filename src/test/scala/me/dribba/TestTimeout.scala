package me.dribba

import akka.actor.Cancellable


case class TestTimeout() {

  private var cancelled = false

  val cancellable = new Cancellable {

    override def isCancelled: Boolean = cancelled

    override def cancel(): Boolean = {
      cancelled = true
      true
    }
  }

}
