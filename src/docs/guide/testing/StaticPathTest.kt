package guide.testing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

val EchoBody: HttpHandler = { r -> Response(OK).body(r.bodyString()) }

class StaticPathTest {
    @Test
    fun `echoes body from request`() {

        Request(GET, "/anything").body("my data is large")
        val response: Response = EchoBody(Request(GET, "/anything").body("my data is large"))

        assertThat(response, hasStatus(OK).and(hasBody("my data is large")))
    }
}
