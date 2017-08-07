package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri.Companion.of
import org.junit.Test

class RouterTest {

    private val notFoundRouter = object : RoutingHttpHandler {
        override fun invoke(p1: Request): Response {
            TODO("not implemented")
        }

        override fun withFilter(new: Filter): RoutingHttpHandler {
            TODO("not implemented")
        }

        override fun withBasePath(new: String): RoutingHttpHandler {
            TODO("not implemented")
        }

        override fun match(request: Request): HttpHandler? = null
    }

    private val okRouter = object : RoutingHttpHandler {
        override fun invoke(p1: Request): Response = Response(OK)

        override fun withFilter(new: Filter): RoutingHttpHandler {
            TODO("not implemented")
        }

        override fun withBasePath(new: String): RoutingHttpHandler {
            TODO("not implemented")
        }

        override fun match(request: Request): HttpHandler? = this
    }

    @Test
    fun `can convert router to handler and call it`() {
        assertThat(routes(okRouter)(Request(GET, of("/boo"))), equalTo(Response(OK)))
    }

    @Test
    fun `falls back to 404 response`() {
        assertThat(routes(notFoundRouter)(Request(GET, of("/boo"))), equalTo(Response(NOT_FOUND)))
    }
}