package org.reekwest.kontrakt

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.body.toBody

class BodyTest {

    private val emptyRequest = get("")

    @Test
    fun `can get string body`() {
        val request = emptyRequest.copy(body = "some value".toBody())
        assertThat(Body.string()(request), equalTo("some value"))
    }

    @Test
    fun `sets value on request`() {
        val body = Body.string.required()
        val withBody = body("hello", emptyRequest)
        assertThat(body(withBody), equalTo("hello"))
    }

    @Test
    fun `can create a custom Body type and get and set on request`() {
        val customBody = Body.string.map(::MyCustomBodyType, { it.value }).required()

        val custom = MyCustomBodyType("hello world!")
        val reqWithBody = customBody(custom, emptyRequest)

        assertThat(reqWithBody.bodyString(), equalTo("hello world!"))

        assertThat(customBody(reqWithBody), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `can create a one way custom Body type`() {
        val customBody = Body.string.map(::MyCustomBodyType).required()
        assertThat(customBody(emptyRequest.bodyString("hello world!")), equalTo(MyCustomBodyType("hello world!")))
    }
}


