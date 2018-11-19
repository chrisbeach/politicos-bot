package com.brightercode.politcobot.util

import scala.language.implicitConversions

object Time {
  implicit def asFiniteDuration(d: java.time.Duration) =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)
}
