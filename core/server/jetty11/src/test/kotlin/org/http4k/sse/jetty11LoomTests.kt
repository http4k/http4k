package org.http4k.sse

import org.http4k.server.Jetty11Loom
import org.http4k.testingStopMode

class Jetty11LoomSseTest : SseServerContract({ Jetty11Loom(it, testingStopMode) })

class Jetty11LoomDatastarTest : DatastarServerContract({ Jetty11Loom(it, testingStopMode) })
