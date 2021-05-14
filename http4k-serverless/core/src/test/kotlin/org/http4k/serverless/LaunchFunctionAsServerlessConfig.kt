package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import java.io.InputStream

class ServerlessConfigTest {

    @Test
    fun `can implement serverless config and respond to requests`() {
        val fnHandler: FnHandler<InputStream, String, InputStream> = FnHandler { i, c ->
            val input = i.reader().readText()
            (input + c + input.reversed()).byteInputStream()
        }

        val server = fnHandler.asServer {
            { req: Request ->
                Response(OK).body(it(emptyMap())(req.body.stream, "!!"))
            }.asServer(SunHttp())
        }

        server.start().use {
            val http = JavaHttpClient()
            assertThat(
                http(Request(GET, "http://localhost:${it.port()}").body("helloworld")),
                hasBody("helloworld!!dlrowolleh")
            )
        }
    }
}
