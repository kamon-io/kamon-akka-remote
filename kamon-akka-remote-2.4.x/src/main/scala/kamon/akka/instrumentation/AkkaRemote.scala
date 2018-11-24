package kamon.akka.instrumentation

import kamon.Kamon

object AkkaRemote {
  val serializationInstrumentation: Boolean = Kamon.config.getBoolean("kamon.akka-remote.serialization-metric")
}
