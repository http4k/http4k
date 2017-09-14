package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import java.util.*

class RequestContextTest {

    @Test
    fun `can set and get a typed value from a RequestContext`() {
        val requestContext = RequestContext(UUID.randomUUID())
        requestContext.set("foo", 123)
        assertThat(requestContext.get("foo"), equalTo(123))
    }

    @Test(expected = KotlinNullPointerException::class)
    fun `throws when missing`() {
        RequestContext(UUID.randomUUID()).get<String>("foo")
    }
}