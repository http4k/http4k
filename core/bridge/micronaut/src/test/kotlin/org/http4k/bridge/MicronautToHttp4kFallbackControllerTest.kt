package org.http4k.bridge

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.micronaut.http.HttpRequestFactory
import io.micronaut.http.annotation.Controller
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import java.io.InputStream

class MicronautToHttp4kFallbackControllerTest {

    @Controller("/")
    class TestController(override val http4k: HttpHandler) : MicronautToHttp4kFallbackController

    @Test
    fun `passes requests through and adapts to servlet`() {
        val controller = TestController { req: Request -> Response(Status.OK).body(req.body).headers(req.headers) }

        val mn = HttpRequestFactory.INSTANCE.get<InputStream>("/bob").header("foo", "bar")
            .body("helloworld".byteInputStream() as InputStream)

        val output = controller.post(mn)

        assertThat(output.status.code, equalTo(200))
        assertThat(output.headers["foo"], equalTo("bar"))
        assertThat(output.body().reader().readText(), equalTo("helloworld"))
    }
}
