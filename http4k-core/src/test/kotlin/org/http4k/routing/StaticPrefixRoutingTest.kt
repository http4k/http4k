package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StaticPrefixRoutingTest {
    private lateinit var app: RoutingHttpHandler

    @BeforeEach
    fun before() {
        app = routes("/bar" bind static(Classpath("bar")))
    }

    @Test
    fun `test static resource bar`() {
        val result = app(Request(GET, "/bar/"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/index.html"))
    }

    @Test
    fun `test static resource bar - index html`() {
        val result = app(Request(GET, "/bar/index.html"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/index.html"))
    }

    @Test
    fun `test static resource bar - bar html`() {
        val result = app(Request(GET, "/bar/bar.html"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/bar.html"))
    }

    @Test
    fun `test static resource bar - bar-xyz html`() {
        val result = app(Request(GET, "/bar/bar-xyz.html"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/bar-xyz.html"))
    }

    @Test
    fun `test static resource bar - bar`() {
        val result = app(Request(GET, "/bar/bar/"))
        assertThat(result.status, equalTo(NOT_FOUND)) // because index.html is read only for root
    }

    @Test
    fun `test static resource bar - bar - index html`() {
        val result = app(Request(GET, "/bar/bar/index.html"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/bar/index.html"))
    }

    @Test
    fun `test static resource bar - bar - bar html`() {
        val result = app(Request(GET, "/bar/bar/bar.html"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/bar/bar.html"))
    }

    @Test
    fun `test static resource bar - bar - bar-xyz html`() {
        val result = app(Request(GET, "/bar/bar/bar-xyz.html"))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("contents of bar/bar/bar-xyz.html"))
    }

}
