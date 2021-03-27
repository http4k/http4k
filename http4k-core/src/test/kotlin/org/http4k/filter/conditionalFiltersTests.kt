package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ThenIfTest {

    private val UserAgentFilter = Filter { next ->
        {
            next(it).header("isBrowser", "true")
        }
    }

    val handler = foobarFilter
        .thenIf({ it.header("referrer") != null }, UserAgentFilter)
        .then {
            assertThat(it, hasHeader("foo"))
            Response(OK)
        }

    @Test
    fun `should run filter when condition is satisfied`() {
        val request = Request(GET, "/employee/1").header("referrer", "firefox")
        val response = handler(request)

        assertThat(response.header("isBrowser"), equalTo("true"))
    }

    @Test
    fun `should not run filter when condition is not satisfied`() {
        val request = Request(GET, "/employee/1")
        val response = handler(request)

        assertNull(response.header("isBrowser"))
    }
}

class ThenNotIfTest {
    private val UnauthorizedUserFilter = Filter { next ->
        {
            next(it).status(UNAUTHORIZED)
        }
    }

    private val authorized: (Request) -> Boolean = { it.query("userName") == "admin" }

    val handler = foobarFilter
        .thenIfNot(authorized, UnauthorizedUserFilter)
        .then {
            assertThat(it, hasHeader("foo"))
            Response(OK)
        }

    @Test
    fun `should run filter when condition is not satisfied`() {
        val request = Request(GET, "/get/sensitive/data").query("userName", "invalid user")
        val response = handler(request)

        assertThat(response.status, equalTo(UNAUTHORIZED))
    }

    @Test
    fun `should not run filter when condition is satisfied`() {
        val request = Request(GET, "/get/sensitive/data").query("userName", "admin")
        val response = handler(request)

        assertThat(response.status, equalTo(OK))
    }
}

private val foobarFilter = Filter { next ->
    {
        next(it.header("foo", "bar"))
    }
}
