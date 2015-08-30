package me.dribba.models


sealed trait Status

object Status {

  object On extends Status

  object Off extends Status
}

