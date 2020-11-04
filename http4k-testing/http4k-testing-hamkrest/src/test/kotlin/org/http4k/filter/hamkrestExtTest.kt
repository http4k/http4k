package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Test

class ExtensionsTest {
    @Test
    fun `request matching as a filter`() {
        val app = RequestFilters.Assert(hasHeader("bob")).then { Response(OK) }

        app(Request(GET, "").header("bob", "foo"))

        assertThat({ app(Request(GET, "")) }, throws<AssertionError>())
    }

    @Test
    fun `response matching as a filter`() {
        ResponseFilters.Assert(!hasHeader("bob")).then { Response(OK) }(Request(GET, ""))

        assertThat({ ResponseFilters.Assert(hasHeader("bob")).then { Response(OK) }(Request(GET, "")) }, throws<AssertionError>())
    }

}
