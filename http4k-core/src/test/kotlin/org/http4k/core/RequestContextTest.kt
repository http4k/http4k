package org.http4k.core

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.UUID

class RequestContextTest {

    @Test
    fun `can set and get a typed value from a RequestContext`() {
        val requestContext = RequestContext(UUID.randomUUID())
        requestContext["foo"] = 123
        assertThat(requestContext["foo"], equalTo(123))
    }

    @Test
    fun `updating a value to null removes it`() {
        val requestContext = RequestContext(UUID.randomUUID())
        requestContext["foo"] = 123
        requestContext["foo"] = null
        assertThat(requestContext["foo"], absent<Int>())
    }

    @Test
    fun `returns null when missing`() {
        assertThat(RequestContext(UUID.randomUUID())["foo"], absent<Int>())
    }
}
