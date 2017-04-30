package org.reekwest.http.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.then

class ClientCookiesTest {
    @Test
    fun `can store and send cookies across multiple calls`() {
        val server = { request: Request -> ok().counterCookie(request.counterCookie() + 1) }

        val client = ClientCookies().then(server)

        (0..3).forEach {
            val response = client(get("/"))
            assertThat(response.header("Set-Cookie"), equalTo("""counter="${it + 1}"; """))
        }
    }

    fun Request.counterCookie() = cookie("counter")?.value?.toInt() ?: 0
    fun Response.counterCookie(value: Int) = cookie(Cookie("counter", value.toString()))
}
