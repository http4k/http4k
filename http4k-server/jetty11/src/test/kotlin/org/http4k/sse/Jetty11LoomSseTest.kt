package org.http4k.sse

import org.http4k.client.JavaHttpClient
import org.http4k.server.Jetty11Loom
import org.http4k.testingStopMode
import org.junit.jupiter.api.Disabled

@Disabled("temporarily disabled")
class Jetty11LoomSseTest :
    SseServerContract({ Jetty11Loom(it, testingStopMode) }, JavaHttpClient())
