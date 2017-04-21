package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.get

class BodyTest {

    private val emptyRequest = get("")

    @Test
    fun `can get string body`() {
        val request = emptyRequest.copy(body = "some value".toBody())
        assertThat(Body.string()(request), equalTo("some value"))
    }

    @Test
    fun `sets value on request`() {
        val body = Body.string.required("body")
        val withBody = body("hello", emptyRequest)
        assertThat(body(withBody), equalTo("hello"))
    }

    @Test
    fun `can create a custom Body type and get and set on request`() {
        val customBody = Body.string.map({ MyCustomBodyType(it) }, { it.value }).required("body")

        val custom = MyCustomBodyType("hello world!")
        val reqWithBody = customBody(custom, emptyRequest)

        assertThat(reqWithBody.bodyString(), equalTo("hello world!"))

        assertThat(customBody(reqWithBody), equalTo(MyCustomBodyType("hello world!")))
    }
}


