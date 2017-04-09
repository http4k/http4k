package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.contract.*
import org.reekwest.http.core.get

class BodyTest {

    @Test
    fun `can get string body`() {
        val request = get("").copy(body = "some value".toBody())
        assertThat(request[Body.string()], equalTo("some value"))
    }

    @Test
    fun `can get form body`() {
        val request = get("").copy(
            headers = listOf("Content-Type" to ContentType.APPLICATION_FORM_URLENCODED.value),
            body = "hello=world&another=planet".toBody())
        val expected: Form = listOf("hello" to "world", "another" to "planet")
        assertThat(request[Body.form()], equalTo(expected))
    }

    @Test
    fun `form body blows up if not URL content type`() {
        val request = get("").copy(
            headers = listOf("Content-Type" to "unknown"),
            body = "hello=world&another=planet".toBody())
        assertThat({request[Body.form()]}, throws<Invalid>())
    }

    data class MyCustomBodyType(val value: String)
//
//    @Test
//    fun `can create a custom Body type`() {
//
//        fun Body.toCustomType() = Body.map(::MyCustomBodyType).string()
//
//        val request = get("").copy(
//            body = "hello world!".toBody())
//        assertThat(request[Body.toCustomType()], equalTo(MyCustomBodyType("hello world!")))
//    }
}


