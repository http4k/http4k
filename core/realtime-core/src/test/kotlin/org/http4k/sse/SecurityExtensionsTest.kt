package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.lens.basicAuthentication
import org.http4k.security.BasicAuthSecurity
import org.junit.jupiter.api.Test

class SecurityExtensionsTest {

    @Test
    fun `passed security returns ok`() {
        val app = SseFilter(BasicAuthSecurity("") { true }).then { SseResponse(OK) {} }
        assertThat(app(Request(GET, "").basicAuthentication(Credentials("", ""))).status, equalTo(OK))
    }

    @Test
    fun `failed security returns error`() {
        val app = SseFilter(BasicAuthSecurity("") { true }).then { SseResponse(OK) {} }
        val response = app(Request(GET, ""))
        assertThat(response.status, equalTo(UNAUTHORIZED))
        assertThat(response.headers, equalTo(listOf("WWW-Authenticate" to """Basic Realm=""""")))
    }
}
