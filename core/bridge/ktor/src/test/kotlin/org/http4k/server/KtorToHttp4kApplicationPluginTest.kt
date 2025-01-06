package org.http4k.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.http4k.bridge.KtorToHttp4kApplicationPlugin
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import java.util.Random

class KtorToHttp4kApplicationPluginTest : PortBasedTest {

    @Test
    fun `translates request to http4k and back again`() {

        val port = Random().nextInt(1000) + 10000

        val engine = embeddedServer(Netty, port) {
            install(
                KtorToHttp4kApplicationPlugin { Response(OK).headers(it.headers).body(it.body) }
            )
        }

        engine.start()

        try {
            val request = Request(POST, "http://localhost:$port")
                .header("foo", "bar")
                .body("hello")
            val response = JavaHttpClient()(request)

            assertThat(response, hasStatus(OK).and(hasBody("hello")).and(hasHeader("foo", "bar")))
        } finally {
            engine.stop()
        }

    }
}
