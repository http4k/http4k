package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11
import org.http4k.server.ServerConfig
import org.http4k.testingStopMode

class Jetty11SseTest : SseServerContract({ Jetty11(it, testingStopMode) }, JavaHttpClient())

class Jetty11DatastarTest : DatastarServerContract({ Jetty11(it, ServerConfig.StopMode.Immediate) }, JavaHttpClient())
