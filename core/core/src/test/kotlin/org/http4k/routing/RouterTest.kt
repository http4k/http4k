package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.Router.Companion.orElse
import org.http4k.routing.RouterDescription.Companion.unavailable
import org.junit.jupiter.api.Test

class RouterTest {

    @Test
    fun `orElse router catches everything`() {
        val asRouter = { _: Request -> false }.asRouter()
        val app = routes(
            asRouter bind { _: Request -> Response(NOT_ACCEPTABLE) },
            orElse bind { _: Request -> Response(OK) }
        )
        assertThat(app(Request(GET, "FOOBAR")), equalTo(Response(OK)))
        assertThat(asRouter.description, equalTo(unavailable))
    }
}
