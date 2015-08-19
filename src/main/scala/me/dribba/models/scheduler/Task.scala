package me.dribba.models.scheduler


trait Task[A] {

  def run: A

}
