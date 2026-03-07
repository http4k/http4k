package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.domain.BodyHydration.All
import org.http4k.wiretap.domain.BodyHydration.None
import org.http4k.wiretap.domain.BodyHydration.RequestOnly
import org.http4k.wiretap.domain.BodyHydration.ResponseOnly
import org.junit.jupiter.api.Test

class BodyHydrationTest {

    private val request = Request(GET, "/test")
    private val response = Response(OK)

    @Test
    fun `All returns true for request`() {
        assertThat(All(request), equalTo(true))
    }

    @Test
    fun `All returns true for response`() {
        assertThat(All(response), equalTo(true))
    }

    @Test
    fun `RequestOnly returns true for request and false for response`() {
        assertThat(RequestOnly(request), equalTo(true))
        assertThat(RequestOnly(response), equalTo(false))
    }

    @Test
    fun `ResponseOnly returns true for response and false for request`() {
        assertThat(ResponseOnly(response), equalTo(true))
        assertThat(ResponseOnly(request), equalTo(false))
    }

    @Test
    fun `None returns false for both`() {
        assertThat(None(request), equalTo(false))
        assertThat(None(response), equalTo(false))
    }
}
