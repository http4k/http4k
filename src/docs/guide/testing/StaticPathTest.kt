package guide.testing

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.Test

object EchoBody {
    operator fun invoke(): HttpHandler = { r -> Response(OK).body(r.bodyString()) }
}

class StaticPathTest {
    @Test
    fun `echoes body from request`() {
        val handler: HttpHandler = EchoBody()

        val response: Response = handler(Request(GET, "/anything").body("my data is large"))

        response.status shouldMatch equalTo(OK)
        response.bodyString() shouldMatch equalTo("my data is large")
    }
}