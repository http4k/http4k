package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri.Companion.of
import org.junit.Test

class RouterTest {

    private val notFoundRouter = object : Router {
        override fun match(request: Request): HttpHandler? = null
    }

    private val okRouter = object : Router {
            override fun match(request: Request): HttpHandler? = { Response(OK) }
    }

    @Test
    fun `can convert router to handler and call it`() {
        assertThat(okRouter.toHttpHandler()(Request(GET, of("/boo"))), equalTo(Response(OK)))
    }

    @Test
    fun `falls back to 404 response`() {
        assertThat(notFoundRouter.toHttpHandler()(Request(GET, of("/boo"))), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun `can combine routers and call them as a handler`() {
        assertThat(notFoundRouter.then(okRouter).toHttpHandler()(Request(GET, of("/boo"))), equalTo(Response(OK)))
    }

}