package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.Test

class HttpMessageAsStringTest {

    @Test
    fun `represents request as string`() {
        val request = Request(Method.GET, Uri.of("http://www.somewhere.com/path"))
            .header("foo", "one").header("bar", "two").body("body".toBody())
        assertThat(request.toString(), equalTo("""
        GET http://www.somewhere.com/path HTTP/1.1
        foo: one
        bar: two

        body""".trimIndent().replace("\n", "\r\n")))
    }

    @Test
    fun `represents response as string`() {
        val request = Response(OK)
            .header("foo", "one").header("bar", "two").body("body".toBody())
        assertThat(request.toString(), equalTo("""
        HTTP/1.1 200 OK
        foo: one
        bar: two

        body""".trimIndent().replace("\n", "\r\n")))
    }
}