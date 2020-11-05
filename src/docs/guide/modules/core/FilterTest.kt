package guide.testing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

val AddLatency = Filter { next ->
    HttpHandler {
        next(it).header("x-extra-header", "some value")
    }
}

class FilterTest {
    @Test
    fun `adds a special header`() {
        val handler: HttpHandler = AddLatency.then { Response(OK) }
        val response: Response = handler(Request(GET, "/echo/my+great+message"))
        assertThat(response, hasStatus(OK).and(hasHeader("x-extra-header", "some value")))
    }
}
