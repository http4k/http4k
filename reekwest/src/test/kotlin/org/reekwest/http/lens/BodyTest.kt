package org.reekwest.http.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.ContentType.Companion.TEXT_PLAIN
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Status.Companion.NOT_ACCEPTABLE
import org.reekwest.http.core.toBody
import org.reekwest.http.lens.Header.Common.CONTENT_TYPE

class BodyTest {

    private val emptyRequest = get("")

    @Test
    fun `can get string body`() {
        val request = emptyRequest.header("Content-type", TEXT_PLAIN.value).copy(body = "some value".toBody())
        assertThat(Body.string(TEXT_PLAIN).required()(request), equalTo("some value"))
    }

    @Test
    fun `rejects invalid or missing content type`() {
        val request = emptyRequest.copy(body = "some value".toBody())
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


