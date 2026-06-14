package org.http4k.webdriver.datastar

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.junit.jupiter.api.Test

class ActionTest {

    @Test
    fun `request carries the datastar-request header`() {
        val request = Action(GET, "/foo").toRequest()
        assertThat(request.method, equalTo(GET))
        assertThat(request.uri.path, equalTo("/foo"))
        assertThat(request.header("datastar-request"), equalTo("true"))
    }

    @Test
    fun `GET sends signals in the datastar query param`() {
        val request = Action(GET, "/foo").toRequest("""{"count":1}""")
        assertThat(request.query("datastar"), equalTo("""{"count":1}"""))
        assertThat(request.bodyString(), equalTo(""))
    }

    @Test
    fun `non-GET sends signals as a JSON body`() {
        val request = Action(POST, "/foo").toRequest("""{"count":1}""")
        assertThat(request.bodyString(), equalTo("""{"count":1}"""))
        assertThat(request.header("Content-Type"), equalTo("application/json; charset=utf-8"))
        assertThat(request.uri.query, equalTo(""))
    }
}
