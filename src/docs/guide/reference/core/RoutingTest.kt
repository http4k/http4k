package guide.testing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

val EchoPath =
    "/echo/{message}" bind GET to { r -> Response(OK).body(r.path("message") ?: "nothing!") }

class DynamicPathTest {

    @Test
    fun `echoes body from path`() {
        val route: RoutingHttpHandler = routes(EchoPath)
        val response: Response = route(Request(GET, "/echo/my%20great%20message"))
        assertThat(response, hasStatus(OK).and(hasBody("my great message")))
    }
}
