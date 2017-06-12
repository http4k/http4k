package guide.testing

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.Route
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.Test

object EchoPath {
    private val handler: HttpHandler = { r -> Response(OK).body(r.path("message") ?: "nothing!") }

    operator fun invoke(): Route = "/echo/{message}" to GET bind handler
}

class DynamicPathTest {

    @Test
    fun `echoes body from path`() {
        val route: RoutingHttpHandler = routes(EchoPath())
        val response: Response = route(Request(GET, "/echo/my+great+message"))
        response.status shouldMatch equalTo(OK)
        response.bodyString() shouldMatch equalTo("my great message")
    }
}