package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.ResponseWithContext
import org.junit.jupiter.api.Test

class ResponseKeyTest {

    private val response = ResponseWithContext(Response(OK), mapOf("hello" to "world"))

    @Test
    fun `value present`() {

        assertThat(ResponseKey.optional("hello")(response), equalTo("world"))
        assertThat(ResponseKey.required("hello")(response), equalTo("world"))

        val expected: List<String?> = listOf("world")
        assertThat(ResponseKey.multi.required("hello")(response), equalTo(expected))
        assertThat(ResponseKey.multi.optional("hello")(response), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(ResponseKey.optional("world")(response), absent())

        val requiredResponseKey = ResponseKey.required("world")
        assertThat(
            { requiredResponseKey(response) },
            throws(lensFailureWith<Response>(Missing(requiredResponseKey.meta), overallType = Failure.Type.Missing))
        )

        assertThat(ResponseKey.multi.optional("world")(response), absent())
        val requiredMultiResponseKey = ResponseKey.multi.required("world")
        assertThat(
            { requiredMultiResponseKey(response) },
            throws(
                lensFailureWith<Response>(
                    Missing(requiredMultiResponseKey.meta),
                    overallType = Failure.Type.Missing
                )
            )
        )
    }

    @Test
    fun `sets value on response`() {
        val key = ResponseKey.required("bob")
        val withResponseKey = response.with(key of "hello")
        assertThat(key(withResponseKey), equalTo("hello"))
    }
}
