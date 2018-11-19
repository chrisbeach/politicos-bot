package com.brightercode.politcobot.util

import java.lang.Thread.sleep

import scala.concurrent.duration.FiniteDuration

trait LoopHelper {
  def loop(pollInterval: FiniteDuration,
           initialErrorBackoffInterval: FiniteDuration,
           errorBackoffFactor: Int = 2,
           onException: Exception => Any = _ => {})
          (operation: => Any): Unit = {

    var errorBackoff: FiniteDuration = initialErrorBackoffInterval

    while (true) {
      try {
        operation
        errorBackoff = initialErrorBackoffInterval
      } catch {
        case e: Exception =>
          onException(e)
          sleep(errorBackoff.toMillis)
          errorBackoff = errorBackoff * errorBackoffFactor
      }
      sleep(pollInterval.toMillis)
    }
  }
}
