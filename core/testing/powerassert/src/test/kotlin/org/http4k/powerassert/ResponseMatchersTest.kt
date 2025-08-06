package org.http4k.powerassert

import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_GATEWAY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.junit.jupiter.api.Test

class ResponseMatchersTest {
    @Test
    fun status() {
        assert(Response(OK).hasStatus(OK))
        assert(!Response(OK).hasStatus(BAD_GATEWAY))
    }

    @Test
    fun `set cookie`() {
        assert(Response(OK).cookie(Cookie("name", "bob")).hasSetCookie(Cookie("name", "bob")))
        assert(!Response(OK).cookie(Cookie("name", "bob")).hasSetCookie(Cookie("name", "bill")))
    }
}