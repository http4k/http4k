package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class PredicateTest {
    @Test
    fun `orElse router catches everything`() {
        val asPredicate = { _: Request -> false }.asPredicate()
        val app = routes(
            asPredicate bind { _: Request -> Response(NOT_ACCEPTABLE) },
            orElse bind { _: Request -> Response(OK) }
        )
        assertThat(app(Request(GET, "FOOBAR")), equalTo(Response(OK)))
//        assertThat(asRouter.description, equalTo(unavailable))
    }
}
