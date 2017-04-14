package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.body.string
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.Body
import org.reekwest.http.core.contract.Invalid
import org.reekwest.http.core.contract.form
import org.reekwest.http.core.contract.string
import org.reekwest.http.core.get

class BodyTest {

    private val emptyRequest = get("")

    @Test
    fun `can get string body`() {
        val request = emptyRequest.copy(body = "some value".toBody())
        assertThat(Body.string()(request), equalTo("some value"))
    }

    @Test
    fun `can get form body`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to ContentType.APPLICATION_FORM_URLENCODED.value),
            body = "hello=world&another=planet".toBody())
        val expected: Form = listOf("hello" to "world", "another" to "planet")
        assertThat(Body.form()(request), equalTo(expected))
    }

    @Test
    fun `form body blows up if not URL content type`() {
        val request = emptyRequest.copy(
            headers = listOf("Content-Type" to "unknown"),
            body = "hello=world&another=planet".toBody())
        assertThat({ Body.form()(request) }, throws<Invalid>())
    }

    @Test
    fun `sets value on request`() {
        val body = Body.string.required()
        val withBody = body("hello", emptyRequest)
        assertThat(body(withBody), equalTo("hello"))
    }

    @Test
    fun `can create a custom Body type and get and set on request`() {
        val customBody = Body.string.map({ MyCustomBodyType(it) }, { it.value }).required()

        val custom = MyCustomBodyType("hello world!")
        val reqWithBody = customBody(custom, emptyRequest)

        assertThat(reqWithBody.body.string(), equalTo("hello world!"))

        assertThat(customBody(reqWithBody), equalTo(MyCustomBodyType("hello world!")))
    }
}


