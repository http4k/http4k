package org.http4k.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.toHttpHandler
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestFiltersTest {

    @Test
    fun `tap passes request through to function`() {
        val get = get("")
        var called = false
        RequestFilters.Tap { called = true; assertThat(it, equalTo(get)) }.then(Response(OK).toHttpHandler())(get)
        assertTrue(called)
    }

}