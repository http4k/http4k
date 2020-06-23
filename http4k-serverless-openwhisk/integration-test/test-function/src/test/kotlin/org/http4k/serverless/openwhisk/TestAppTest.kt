package org.http4k.serverless.openwhisk

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class TestAppTest {

    @Test
    fun `can invoke handler`() {
        val app = TestApp(emptyMap())

        assertThat(app(Request(POST, "/echo").body("hello")), hasStatus(Status.OK) and hasBody("hello"))
    }
}
