package org.http4k.testing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.binary
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.io.ByteArrayInputStream
import java.time.Duration

class TestApp() {
    val allRoutes =
        routes(
            "/health" bind Method.GET to { Response(Status.OK).body("UP") },
            "/slow-echo" bind Method.POST to slowEchoHandler(Duration.ofMillis(500))
        )

    private fun slowEchoHandler(delay: Duration): (Request) -> Response = {
        busyWait(delay)
        Response(Status.OK).with(
            Body.binary(ContentType.TEXT_PLAIN).toLens() of SlowBodyProducer(
                delay,
                it.bodyString()
            )
        )
    }

    private inner class SlowBodyProducer(val delay: Duration, message: String) :
        ByteArrayInputStream(message.toByteArray()) {
        var firstRead = true
        fun <T> wait(then: () -> T): T = then().also {
            if (firstRead) {
                busyWait(delay)
                firstRead = false
            }
        }

        override fun read(): Int = wait { super.read() }
        override fun read(b: ByteArray): Int = wait { super.read(b) }
        override fun read(b: ByteArray, off: Int, len: Int): Int = wait { super.read(b, off, len) }
    }

    private fun busyWait(delay: Duration) {
        val continueAt = System.currentTimeMillis() + delay.toMillis()
        @Suppress("ControlFlowWithEmptyBody")
        while (System.currentTimeMillis() < continueAt) {
        }
    }
}
