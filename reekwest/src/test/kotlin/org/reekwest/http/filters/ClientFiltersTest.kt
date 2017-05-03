package org.reekwest.http.filters

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import junit.framework.Assert.fail
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Request.Companion.post
import org.reekwest.http.core.Request.Companion.put
import org.reekwest.http.core.Response.Companion.movedPermanently
import org.reekwest.http.core.Response.Companion.movedTemporarily
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.Response.Companion.serverError
import org.reekwest.http.core.then

class ClientFiltersTest {
    val server = { request: Request ->
        when (request.uri.path) {
            "/redirect" -> movedTemporarily(listOf("location" to "/ok"))
            "/loop" -> movedTemporarily(listOf("location" to "/loop"))
            "/absolute-target" -> if (request.uri.host == "example.com") ok().body("absolute") else serverError()
            "/absolute-redirect" -> movedPermanently(listOf("location" to "http://example.com/absolute-target"))
            else -> ok().let { if (request.query("foo") != null) it.body("with query") else it }
        }
    }

    val client = ClientFilters.FollowRedirects().then(server)

    @Test
    fun `does not follow redirect by default`() {
        val defaultClient = server
        assertThat(defaultClient(get("/redirect")), equalTo(movedTemporarily(listOf("location" to "/ok"))))
    }

    @Test
    fun `follows redirect for temporary redirect response`() {
        assertThat(client(get("/redirect")), equalTo(ok()))
    }

    @Test
    fun `does not follow redirect for post`() {
        assertThat(client(post("/redirect")), equalTo(movedTemporarily(listOf("location" to "/ok"))))
    }

    @Test
    fun `does not follow redirect for put`() {
        assertThat(client(put("/redirect")), equalTo(movedTemporarily(listOf("location" to "/ok"))))
    }

    @Test
    fun `supports absolute redirects`() {
        assertThat(client(get("/absolute-redirect")), equalTo(ok().body("absolute")))
    }

    @Test
    fun `discards query parameters in relative redirects`() {
        assertThat(client(get("/redirect?foo=bar")), equalTo(ok()))
    }

    @Test
    fun `prevents redirection loop after 10 redirects`() {
        try {
            client(get("/loop"))
            fail("should have looped")
        } catch (e: IllegalStateException) {
            assertThat(e.message, equalTo("Too many redirection"))
        }
    }
}