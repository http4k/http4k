package org.http4k.lens

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

    private val key = RequestContextKey.of<String>(contexts)

    private val request = contexts.inject(RequestContext(UUID.randomUUID()), Request(Method.GET, ""))

    @Test
    fun `can use a request context key to set and get a typed value from the request`() {

        key.inject("hello", request)

        key.extract(request) shouldMatch equalTo("hello")
    }

    @Test
    fun `attempt to get a non-existing key throws`() {
        assertThat({ key.extract(request) }, throws<LensFailure>())
    }

}