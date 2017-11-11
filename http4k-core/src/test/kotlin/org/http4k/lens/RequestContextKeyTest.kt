package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.RequestContexts
import org.junit.Test
import java.util.*

class RequestContextKeyTest {
    private val contexts = RequestContexts()

    private val request = contexts(RequestContext(UUID.randomUUID()), Request(Method.GET, ""))

    @Test
    fun `required key behaviour`() {
        val key = RequestContextKey.required<String>(contexts)
        assertThat({ key(request) }, throws(targetIsA<RequestContext>()))
        key("hello", request)
        key(request) shouldMatch equalTo("hello")
    }

    @Test
    fun `optional key behaviour`() {
        val key = RequestContextKey.optional<String>(contexts)
        key(request) shouldMatch absent()
        key("hello", request)
        key(request) shouldMatch equalTo("hello")
    }

    @Test
    fun `defaulted key behaviour`() {
        val key = RequestContextKey.defaulted(contexts, "world")
        key(request) shouldMatch equalTo("world")
        key("hello", request)
        key(request) shouldMatch equalTo("hello")
    }
}