package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.with
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.ContentNegotiation
import org.http4k.lens.Header
import org.http4k.lens.string
import org.junit.jupiter.api.Test

class AutoContentNegotiatorTest {

    private val v1Lens = Body.string(ContentType("v1"))
        .map({ it.replace("v1-", "") }, { "v1-$it" })
        .toLens()

    private val v2Lens = Body.string(ContentType("v2"))
        .map({ it.replace("v2-", "") }, { "v2-$it" })
        .toLens()

    private val negotiator = ContentNegotiation.auto(v1Lens, v2Lens)

    private val returnJohn: HttpHandler = {
        Response(OK).with(negotiator.outbound(it) of "john")
    }

    private val receiveJohn: HttpHandler = { req ->
        val content = negotiator(req)
        Response(if (content == "john") OK else BAD_REQUEST)
    }

    @Test
    fun `response body - without accept header`() {
        val response = Request(GET, "/")
            .let(returnJohn)

        assertThat(response, hasBody("v1-john"))
    }

    @Test
    fun `response body - with v1 accept header`() {
        val response = Request(GET, "/")
            .header("accept", v1Lens.contentType.toHeaderValue())
            .let(returnJohn)

        assertThat(response, hasBody("v1-john"))
    }

    @Test
    fun `response body - with v2 accept header`() {
        val response = Request(GET, "/")
            .header("accept", v2Lens.contentType.toHeaderValue())
            .let(returnJohn)

        assertThat(response, hasBody("v2-john"))
    }

    @Test
    fun `request body - v1 body without content-type header`() {
        val response = Request(GET, "/")
            .body("v1-john")
            .let(receiveJohn)

        assertThat(response, hasStatus(OK))
    }

    @Test
    fun `request body - v2 body without content-type header`() {
        val response = Request(GET, "/")
            .body("v2-john")
            .let(receiveJohn)

        assertThat(response, hasStatus(BAD_REQUEST))
    }

    @Test
    fun `request body - v1 body`() {
        val response = Request(GET, "/")
            .with(v1Lens of "john")
            .with(Header.CONTENT_TYPE of v1Lens.contentType)
            .let(receiveJohn)

        assertThat(response, hasStatus(OK))
    }

    @Test
    fun `request body - v2 body`() {
        val response = Request(GET, "/")
            .with(v2Lens of "john")
            .let(receiveJohn)

        assertThat(response, hasStatus(OK))
    }

    @Test
    fun `request body - v1 body with v2 content-type header`() {
        val response = Request(GET, "/")
            .with(v1Lens of "john")
            .with(Header.CONTENT_TYPE of v2Lens.contentType)
            .let(receiveJohn)

        assertThat(response, hasStatus(BAD_REQUEST))
    }
}
