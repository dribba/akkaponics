package me.dribba.models.scheduler


trait Subscription[A] {

  def cancel: Unit

}
