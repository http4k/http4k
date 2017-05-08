package org.http4k.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.core.then
import org.http4k.http.toHttpHandler

class RequestFiltersTest {

    @Test
    fun `tap passes request through to function`() {
        val get = get("")
        var called = false
        RequestFilters.Tap { called = true; assertThat(it, equalTo(get)) }.then(Response(OK).toHttpHandler())(get)
        assertTrue(called)
    }

}