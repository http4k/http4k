package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class NoSecurityTest {
    @Test
    fun `no security is rather lax`() {
        val response = (NoSecurity.filter { Response(Status.OK).body("hello") })(Request(Method.GET, ""))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }
}