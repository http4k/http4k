package org.http4k.kotest

import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ResponseMatchersTest {
    @Test
    fun status() = assertMatchAndNonMatch(Response(OK), { shouldHaveStatus(OK) }, { shouldHaveStatus(BAD_GATEWAY) })

    @Test
    fun `set cookie`() = assertMatchAndNonMatch(Response(OK).cookie(Cookie("name", "bob")), haveSetCookie(Cookie("name", "bob")), haveSetCookie(Cookie("name", "bill")))

    @Test
    fun `should haveSetCookie(Cookie), cookie does not exist`() {
        assertThrows<AssertionError> {
            Response(OK).cookie(Cookie("name", "bob")) should haveSetCookie(Cookie("planet", "Earth"))
        }
    }

    @Test
    fun `shouldNot haveSetCookie(Cookie), cookie does not exist`() {
        assertDoesNotThrow {
            Response(OK).cookie(Cookie("name", "bob")) shouldNot haveSetCookie(Cookie("planet", "Earth"))
        }
    }
}
