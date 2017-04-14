package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.Body
import org.reekwest.http.core.contract.Invalid
import org.reekwest.http.core.contract.form
import org.reekwest.http.core.contract.string
import org.reekwest.http.core.get

class BodyTest {

    @Test
    fun `can get string body`() {
        val request = get("").copy(body = "some value".toBody())
        assertThat(Body.string()(request), equalTo("some value"))
    }

    @Test
    fun `can get form body`() {
        val request = get("").copy(
            headers = listOf("Content-Type" to ContentType.APPLICATION_FORM_URLENCODED.value),
            body = "hello=world&another=planet".toBody())
        val expected: Form = listOf("hello" to "world", "another" to "planet")
        assertThat(Body.form()(request), equalTo(expected))
    }

    @Test
    fun `form body blows up if not URL content type`() {
        val request = get("").copy(
            headers = listOf("Content-Type" to "unknown"),
            body = "hello=world&another=planet".toBody())
        assertThat({ Body.form()(request) }, throws<Invalid>())
    }

    @Test
    fun `sets value on request`() {
        val body = Body.string.get()
        val withBody = body(get(""), "hello")
        assertThat(body(withBody), equalTo("hello"))
    }

    data class MyCustomBodyType(val value: String)

    @Test
    fun `can create a custom Body type`() {
        fun Body.toCustomType() = Body.string.map(::MyCustomBodyType).get("bob")

        val request = get("").copy(
            body = "hello world!".toBody())
        assertThat(Body.toCustomType()(request), equalTo(MyCustomBodyType("hello world!")))
    }
}


