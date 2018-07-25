package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.client.asClientError
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.junit.jupiter.api.Test

class StatusTest {

    @Test
    fun `can override description`() {
        val description = Status.OK.description("all good")
        assertThat(description.description, equalTo("all good"))
        assertThat(description.toString(), equalTo("200 all good"))
    }

    @Test
    fun `equality does not include cause`() {
        assertThat(SERVICE_UNAVAILABLE.asClientError(RuntimeException("foo")) == SERVICE_UNAVAILABLE.asClientError(RuntimeException("bar")), equalTo(true))
    }

    @Test
    fun `asClientError blows up when not reassigning a server error`() {
        assertThat({OK.asClientError(RuntimeException("foo")) }, throws<IllegalArgumentException>())
    }
}