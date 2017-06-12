package guide.testing

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.Test

val AddLatency = Filter {
    next ->
    {
        next(it).header("x-extra-header", "some value")
    }
}

class FilterTest {
    @Test
    fun `adds a special header`() {
        val handler: HttpHandler = AddLatency.then { Response(OK) }
        val response: Response = handler(Request(GET, "/echo/my+great+message"))
        response.status shouldMatch equalTo(OK)
        response.header("x-extra-header") shouldMatch equalTo("some value")
    }
}