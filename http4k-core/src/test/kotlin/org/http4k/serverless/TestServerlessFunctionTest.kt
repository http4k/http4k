package org.http4k.serverless

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class TestServerlessFunctionTest {
    @Test
    fun `can invoke handler`() {
        val app = TestServerlessFunction(emptyMap())

        assertThat(app(Request(Method.POST, "/echo").body("hello")), hasStatus(Status.OK) and hasBody("hello"))
    }
}
