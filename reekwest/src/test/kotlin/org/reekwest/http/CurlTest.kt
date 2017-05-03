package org.reekwest.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Request.Companion.post
import org.reekwest.http.core.body
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.bodyString
import org.reekwest.http.core.toCurl

class CurlTest {

    @Test
    fun `generates for simple get request`() {
        val curl = get("http://httpbin.org").toCurl()
        assertThat(curl, equalTo("curl -X GET \"http://httpbin.org\""))
    }

    @Test
    fun `generates for request with query`() {
        val curl = get("http://httpbin.org").query("a", "one two three").toCurl()
        assertThat(curl, equalTo("""curl -X GET "http://httpbin.org?a=one+two+three""""))
    }

    @Test
    fun `includes headers`() {
        val curl = get("http://httpbin.org").header("foo", "my header").toCurl()
        assertThat(curl, equalTo("""curl -X GET -H "foo:my header" "http://httpbin.org""""))
    }

    @Test
    fun `includes body data`() {
        val curl = post("http://httpbin.org/post").body(listOf("foo" to "bar").toBody()).toCurl()
        assertThat(curl, equalTo("""curl -X POST --data "foo=bar" "http://httpbin.org/post""""))
    }

    @Test
    fun `escapes body form`() {
        val curl = get("http://httpbin.org").body(listOf("foo" to "bar \"quoted\"").toBody()).toCurl()
        assertThat(curl, equalTo("""curl -X GET --data "foo=bar+%22quoted%22" "http://httpbin.org""""))
    }

    @Test
    fun `escapes body string`() {
        val curl = get("http://httpbin.org").bodyString("my \"quote\"").toCurl()
        assertThat(curl, equalTo("""curl -X GET --data "my \"quote\"" "http://httpbin.org""""))
    }

    @Test
    fun `limits the entity if it's too large`() {
        val largeBody = (0..500).joinToString(" ")
        val curl = get("http://httpbin.org").bodyString(largeBody).toCurl()
        val data = "data \"([^\"]+)\"".toRegex().find(curl)?.groupValues?.get(1)!!
        assertThat(data.length, equalTo(256 + "[truncated]".length))
    }
}