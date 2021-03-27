package org.http4k.filter

import io.kotest.assertions.shouldFail
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.kotest.haveHeader
import org.junit.jupiter.api.Test

class ExtensionsTest {
    @Test
    fun `request matching as a filter`() {
        val app = RequestFilters.Assert(haveHeader("bob")).then { Response(OK) }

        app(Request(GET, "").header("bob", "foo"))

        shouldFail { app(Request(GET, "")) }
    }

    @Test
    fun `response matching as a filter`() {
        ResponseFilters.Assert(haveHeader("bob").invert()).then { Response(OK) }(Request(GET, ""))

        shouldFail { ResponseFilters.Assert(haveHeader("bob")).then { Response(OK) }(Request(GET, "")) }
    }
}
