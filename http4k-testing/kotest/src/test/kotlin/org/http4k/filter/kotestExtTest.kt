package org.http4k.filter

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.kotest.haveHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ExtensionsTest {
    @Test
    fun `request matching as a filter`() {
        val app = RequestFilters.Assert(haveHeader("bob")).then { Response(OK) }

        app(Request(GET, "").header("bob", "foo"))

        assertThrows<AssertionError> { app(Request(GET, "")) }
    }

    @Test
    fun `response matching as a filter`() {
        ResponseFilters.Assert(haveHeader("bob").invert()).then { Response(OK) }(Request(GET, ""))

        assertThrows<AssertionError>  { ResponseFilters.Assert(haveHeader("bob")).then { Response(OK) }(Request(GET, "")) }
    }

    @Test
    fun `request not matching as a filter`() {
        val app = RequestFilters.AssertNot(haveHeader("bob")).then { Response(OK) }

        app(Request(GET, ""))

        assertThrows<AssertionError>  { app(Request(GET, "").header("bob", "foo")) }
    }

    @Test
    fun `response not matching as a filter`() {
        ResponseFilters.AssertNot(haveHeader("bob")).then { Response(OK) }(Request(GET, ""))

        assertThrows<AssertionError>  { ResponseFilters.AssertNot(haveHeader("bob").invert()).then { Response(OK) }(Request(GET, "")) }
    }
}
