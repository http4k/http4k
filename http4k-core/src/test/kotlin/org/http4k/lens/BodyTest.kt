package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Request.Companion.get
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.junit.Test

class BodyTest {

    private val emptyRequest = get("")

    @Test
    fun `can get string body`() {
        val request = emptyRequest.header("Content-type", TEXT_PLAIN.value).body("some value")
        assertThat(Body.string(TEXT_PLAIN).required()(request), equalTo("some value"))
    }

    @Test
    fun `rejects invalid or missing content type`() {
        val request = emptyRequest.body("some value")
        assertThat({ Body.string(TEXT_PLAIN).required()(request) },
            throws(equalTo(LensFailure(CONTENT_TYPE.invalid(), status = NOT_ACCEPTABLE))))
    }

    @Test
    fun `sets value on request`() {
        val body = Body.string(TEXT_PLAIN).required()
        val withBody = body("hello", emptyRequest)
        assertThat(body(withBody), equalTo("hello"))
        assertThat(CONTENT_TYPE(withBody), equalTo(TEXT_PLAIN))
    }

    @Test
    fun `can create a custom Body type and get and set on request`() {
        val customBody = Body.string(TEXT_PLAIN).map(::MyCustomBodyType, { it.value }).required()

        val custom = MyCustomBodyType("hello world!")
        val reqWithBody = customBody(custom, emptyRequest)

        assertThat(reqWithBody.bodyString(), equalTo("hello world!"))

        assertThat(customBody(reqWithBody), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `can create a one way custom Body type`() {
        val customBody = Body.string(TEXT_PLAIN).map(::MyCustomBodyType).required()
        assertThat(customBody(emptyRequest
            .header("Content-type", TEXT_PLAIN.value)
            .body("hello world!")), equalTo(MyCustomBodyType("hello world!")))
    }
}


