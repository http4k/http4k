package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11Loom

class Jetty11LoomSseTest : SseServerContract(::Jetty11Loom, JavaHttpClient())
