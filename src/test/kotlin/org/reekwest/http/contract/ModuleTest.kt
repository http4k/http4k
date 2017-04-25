package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri

class ModuleTest {

    private val notFoundModule = object : Module {
        override fun toHandlerMatcher(): HandlerMatcher = {
            null
        }
    }
    private val okModule = object : Module {
        override fun toHandlerMatcher(): HandlerMatcher = {
            { Response(OK) }
        }
    }

    @Test
    fun `can convert module to handler and call it`() {
        assertThat(okModule.toHttpHandler()(Request(Method.GET, Uri.uri("/boo"))), equalTo(Response(OK)))
    }

    @Test
    fun `falls back to 404 response`() {
        assertThat(notFoundModule.toHttpHandler()(Request(Method.GET, Uri.uri("/boo"))), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun `can combine modules and call them as a handler`() {
        assertThat(notFoundModule.then(okModule).toHttpHandler()(Request(Method.GET, Uri.uri("/boo"))), equalTo(Response(OK)))
    }

}