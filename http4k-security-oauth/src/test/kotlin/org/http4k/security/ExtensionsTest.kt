package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.util.Failure
import org.http4k.util.Result
import org.http4k.util.Success
import org.http4k.util.asResult4k
import org.junit.jupiter.api.Test

class ExtensionsTest {

    @Test
    fun asResult4k() {
        val exception = RuntimeException()
        val value = "hello"
        val failure: Result<Exception, String> = Failure(exception)
        val success: Result<Exception, String> = Success(value)

        assertThat(success.asResult4k, equalTo(com.natpryce.Success(value) as com.natpryce.Result<String, Exception>))
        assertThat(failure.asResult4k, equalTo(com.natpryce.Failure(exception) as com.natpryce.Result<String, Exception>))
    }
}
