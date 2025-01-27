package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.routing.RequestWithContext
import org.junit.jupiter.api.Test

class RequestKeyTest {

    private val request = RequestWithContext(Request(GET, ""), mapOf("hello" to "world"))

    @Test
    fun `value present`() {

        assertThat(RequestKey.optional("hello")(request), equalTo("world"))
        assertThat(RequestKey.required("hello")(request), equalTo("world"))

        val expected: List<String?> = listOf("world")
        assertThat(RequestKey.multi.required("hello")(request), equalTo(expected))
        assertThat(RequestKey.multi.optional("hello")(request), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(RequestKey.optional("world")(request), absent())

        val requiredRequestKey = RequestKey.required("world")
        assertThat(
            { requiredRequestKey(request) },
            throws(lensFailureWith<Request>(Missing(requiredRequestKey.meta), overallType = Failure.Type.Missing))
        )

        assertThat(RequestKey.multi.optional("world")(request), absent())
        val requiredMultiRequestKey = RequestKey.multi.required("world")
        assertThat(
            { requiredMultiRequestKey(request) },
            throws(lensFailureWith<Request>(Missing(requiredMultiRequestKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `sets value on request`() {
        val key = RequestKey.required("bob")
        val withRequestKey = request.with(key of "hello")
        assertThat(key(withRequestKey), equalTo("hello"))
    }

}
