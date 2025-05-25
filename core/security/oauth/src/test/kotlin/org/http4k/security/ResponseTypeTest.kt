package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ResponseTypeTest {
    @Test
    fun `can resolve from query parameter value`() = runBlocking {
        ResponseType.entries.forEach { responseType ->
            assertThat(ResponseType.fromQueryParameterValue(responseType.queryParameterValue), equalTo(responseType))
        }
    }

    @Test
    fun `throws exception if query parameter value is invalid`() = runBlocking {
        val exception = assertThrows<IllegalArgumentException> { ResponseType.fromQueryParameterValue("cookie"); Unit }
        assertThat(exception.message, equalTo("Invalid response type: cookie"))
    }
}
