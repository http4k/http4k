package org.http4k.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import org.http4k.bridge.fallbackToHttp4k
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

class VertxToHttp4kHandlerTest : PortBasedTest {

    @Test
    fun `passes requests through and adapts`() {
        val vertx = Vertx.builder().build()

        val router = Router.router(vertx)
            .apply {
                fallbackToHttp4k { req: Request ->
                    Response(OK)
                        .headers(req.headers)
                        .body(req.body)
                }
            }

        val requestHandler = vertx.createHttpServer()
            .requestHandler(router)
            .apply { listen(0) }

        try {

            val request = Request(POST, "http://localhost:${requestHandler.actualPort()}")
                .header("foo", "bar")
                .body("hello")
            val response = JavaHttpClient()(request)

            assertThat(response, hasStatus(OK).and(hasBody("hello")).and(hasHeader("foo", "bar")))
        } finally {
            requestHandler.close()
        }
    }
}
