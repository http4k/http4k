package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.security.NoSecurity
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status
import org.http4k.core.then
import org.junit.jupiter.api.Test

class NoSecurityTest {
    @Test
    fun `no security is rather lax`() {
        val response = (NoSecurity.filter.then { Response(OK).body("hello") }(Request(GET, "")))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("hello"))
    }
}
