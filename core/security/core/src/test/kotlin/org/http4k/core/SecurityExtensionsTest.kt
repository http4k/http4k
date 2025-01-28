package org.http4k.core

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.basicAuthentication
import org.http4k.security.BasicAuthSecurity
import org.junit.jupiter.api.Test

class SecurityExtensionsTest {

    @Test
    fun `passed security returns ok`() {
        val app = Filter(BasicAuthSecurity("") { true }).then { Response(OK) }
        assertThat(app(Request(GET, "").basicAuthentication(Credentials("", ""))), hasStatus(OK))
    }

    @Test
    fun `failed security returns error`() {
        val app = Filter(BasicAuthSecurity("") { false }).then { Response(OK) }
        assertThat(app(Request(GET, "").basicAuthentication(Credentials("", ""))), hasStatus(UNAUTHORIZED).and(hasHeader("WWW-Authenticate", """Basic Realm=""""")))
    }
}
