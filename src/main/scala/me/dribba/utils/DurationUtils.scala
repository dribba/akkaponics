package me.dribba.utils

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration.FiniteDuration


object DurationUtils {
  
  
  def finiteFromConfig(path: String, unit: TimeUnit = TimeUnit.MILLISECONDS)(implicit config: Config): FiniteDuration =
    FiniteDuration(config.getDuration(path, unit), unit)

}
