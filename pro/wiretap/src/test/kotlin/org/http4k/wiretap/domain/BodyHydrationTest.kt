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

    private val request = Request(GET, "/")
    private val response = Response(OK)

    @Test
    fun `All hydrates both requests and responses`() {
        assertThat(All(request), equalTo(true))
        assertThat(All(response), equalTo(true))
    }

    @Test
    fun `RequestOnly hydrates only requests`() {
        assertThat(RequestOnly(request), equalTo(true))
        assertThat(RequestOnly(response), equalTo(false))
    }

    @Test
    fun `ResponseOnly hydrates only responses`() {
        assertThat(ResponseOnly(request), equalTo(false))
        assertThat(ResponseOnly(response), equalTo(true))
    }

    @Test
    fun `None hydrates nothing`() {
        assertThat(None(request), equalTo(false))
        assertThat(None(response), equalTo(false))
    }
}
