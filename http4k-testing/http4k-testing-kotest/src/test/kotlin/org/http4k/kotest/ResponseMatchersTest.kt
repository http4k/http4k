package org.http4k.kotest

import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.junit.jupiter.api.Test

class ResponseMatchersTest {
    @Test
    fun `status`() = assertMatchAndNonMatch(Response(OK), haveStatus(OK), haveStatus(BAD_GATEWAY))

    @Test
    fun `set cookie`() = assertMatchAndNonMatch(Response(OK).cookie(Cookie("name", "bob")), haveSetCookie(Cookie("name", "bob")), haveSetCookie(Cookie("name", "bill")))
}
