package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.RequestContexts
import org.junit.jupiter.api.Test
import java.util.UUID


class RequestContextKeyTest {
    private val contexts = RequestContexts()

    private val request = contexts(RequestContext(UUID.randomUUID()), Request(GET, ""))

    @Test
    fun `required key behaviour`() {
        val key = RequestContextKey.required<String>(contexts)
        assertThat({ key(request) }, throws(targetIsA<RequestContext>()))
        key("hello", request)
        assertThat(key(request), equalTo("hello"))
    }

    @Test
    fun `optional key behaviour`() {
        val key = RequestContextKey.optional<String>(contexts)
        assertThat(key(request), absent())
        key("hello", request)
        assertThat(key(request), equalTo("hello"))
    }

    @Test
    fun `defaulted key behaviour`() {
        val key = RequestContextKey.defaulted(contexts, "world")
        assertThat(key(request), equalTo("world"))
        key("hello", request)
        assertThat(key(request), equalTo("hello"))
    }
}
