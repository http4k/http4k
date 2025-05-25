package org.http4k.server

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.testingStopMode
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.io.File

class Jetty11Http2Test: PortBasedTest {

    @Test
    fun `can configure http2`() = runBlocking {
        val server = { _: Request -> Response(Status.OK) }.asServer(
            Jetty11(
                0,
                testingStopMode,
                http2(
                    0,
                    File("src/test/resources/keystore.jks").absolutePath,
                    "password"
                )
            )
        )
        server.start().stop()
    }
}
