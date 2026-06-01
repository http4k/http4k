package org.http4k.storyboard.datastar

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.junit.jupiter.api.Test

class ActionTest {

    @Test
    fun `parses @get`() {
        assertThat(parseAction("@get('/foo')"), equalTo(Action(GET, "/foo")))
    }

    @Test
    fun `parses @post`() {
        assertThat(parseAction("@post('/users')"), equalTo(Action(POST, "/users")))
    }

    @Test
    fun `parses @put`() {
        assertThat(parseAction("@put('/x/1')"), equalTo(Action(PUT, "/x/1")))
    }

    @Test
    fun `parses @delete`() {
        assertThat(parseAction("@delete('/x/1')"), equalTo(Action(DELETE, "/x/1")))
    }

    @Test
    fun `tolerates surrounding whitespace`() {
        assertThat(parseAction("  @get('/foo')  "), equalTo(Action(GET, "/foo")))
    }

    @Test
    fun `returns null for unknown expression`() {
        assertThat(parseAction("\$signal"), absent())
        assertThat(parseAction("@patch('/x')"), absent())
        assertThat(parseAction(""), absent())
    }

    @Test
    fun `toRequest marks the datastar-request header`() {
        val request = Action(GET, "/foo").toRequest()
        assertThat(request.method, equalTo(GET))
        assertThat(request.uri.path, equalTo("/foo"))
        assertThat(request.header("datastar-request"), equalTo("true"))
    }
}
