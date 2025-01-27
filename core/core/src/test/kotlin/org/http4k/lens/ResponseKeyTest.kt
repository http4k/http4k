package org.http4k.lens

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
        assertThat(ResponseKey.of<String>("hello")(response), equalTo("world"))
    }

    @Test
    fun `value missing`() {
        val requiredResponseKey = ResponseKey.of<String>("world")
        assertThat(
            { requiredResponseKey(response) },
            throws(lensFailureWith<Response>(Missing(requiredResponseKey.meta), overallType = Failure.Type.Missing))
        )
    }

    @Test
    fun `sets value on response`() {
        val key = ResponseKey.of<String>("bob")
        val withResponseKey = response.with(key of "hello")
        assertThat(key(withResponseKey), equalTo("hello"))
    }
}
