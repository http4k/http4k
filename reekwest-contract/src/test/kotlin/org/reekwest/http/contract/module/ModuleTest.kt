package org.reekwest.http.contract.module

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.lens.LensFailure

class ModuleTest {

    private val notFoundModule = object : Module {
        override fun toRouter(): Router = {
            null
        }
    }

    private val lensFailureModule = object : Module {
        override fun toRouter(): Router = {
            { throw LensFailure() }
        }
    }
    private val okModule = object : Module {
        override fun toRouter(): Router = {
            { Response(OK) }
        }
    }

    @Test
    fun `can convert module to handler and call it`() {
        assertThat(okModule.toHttpHandler()(Request(GET, uri("/boo"))), equalTo(Response(OK)))
    }

    @Test
    fun `falls back to 404 response`() {
        assertThat(notFoundModule.toHttpHandler()(Request(GET, uri("/boo"))), equalTo(Response(NOT_FOUND)))
    }

    @Test
    fun `lens failure results in 400`() {
        assertThat(lensFailureModule.toHttpHandler()(Request(GET, uri("/boo"))), equalTo(Response(BAD_REQUEST)))
    }

    @Test
    fun `can combine modules and call them as a handler`() {
        assertThat(notFoundModule.then(okModule).toHttpHandler()(Request(GET, uri("/boo"))), equalTo(Response(OK)))
    }

}