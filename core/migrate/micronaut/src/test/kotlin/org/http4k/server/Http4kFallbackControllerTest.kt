package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.micronaut.http.HttpRequestFactory
import io.micronaut.http.annotation.Controller
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.io.InputStream

class Http4kFallbackControllerTest {

    @Controller("/")
    class TestController(override val http4k: HttpHandler) : Http4kFallbackController

    @Test
    fun `passes requests through and adapts to servlet`() {
        val controller = TestController { req: Request -> Response(OK).body(req.body).headers(req.headers) }

        val mn = HttpRequestFactory.INSTANCE.get<InputStream>("/bob").header("foo", "bar")
            .body("helloworld".byteInputStream() as InputStream)

        val output = controller.post(mn)

        assertThat(output.status.code, equalTo(200))
        assertThat(output.headers["foo"], equalTo("bar"))
        assertThat(output.body().reader().readText(), equalTo("helloworld"))
    }
}
