package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.MultipartFormWithBoundary
import org.junit.jupiter.api.Test

class ContentTypeTest {

    private val contentType0 = ContentType("none")
    private val contentType1 = ContentType("foo", listOf("bob" to "bill"))
    private val contentType2 = ContentType("foo", listOf("bob" to "jim"))
    private val contentType3 = ContentType("bar", listOf("bob" to null, "bob2" to "jim"))

    @Test
    fun `compare content types without directive`() {
        assertThat(contentType0.equalsIgnoringDirectives(contentType0), equalTo(true))
        assertThat(contentType1.equalsIgnoringDirectives(contentType1), equalTo(true))
        assertThat(contentType1.equalsIgnoringDirectives(contentType2), equalTo(true))
        assertThat(contentType1.equalsIgnoringDirectives(contentType3), equalTo(false))
    }

    @Test
    fun `directive to header value`() {
        assertThat(ContentType.TEXT_PLAIN.toHeaderValue(), equalTo("text/plain; charset=utf-8"))
        assertThat(MultipartFormWithBoundary("foobar").toHeaderValue(), equalTo("multipart/form-data; boundary=foobar"))
        assertThat(contentType1.toHeaderValue(), equalTo("foo; bob=bill"))
        assertThat(contentType2.toHeaderValue(), equalTo("foo; bob=jim"))
        assertThat(contentType3.toHeaderValue(), equalTo("bar; bob; bob2=jim"))
        assertThat(contentType3.toHeaderValue(), equalTo("bar; bob; bob2=jim"))
    }

    @Test
    fun `structure returns null when there is no +suffix`() {
        assertThat(ContentType("foo").structure(), equalTo(null))
        assertThat(ContentType("foo/bar").structure(), equalTo(null))
        assertThat(ContentType("foo/bar.baz").structure(), equalTo(null))
        assertThat(ContentType("foo/bar.baz", listOf("x" to "y")).structure(), equalTo(null))
    }

    @Test
    fun `structure returns the structure suffix when there is a +suffix`() {
        assertThat(ContentType("foo+json").structure(), equalTo("json"))
        assertThat(ContentType("foo/bar+xml").structure(), equalTo("xml"))
        assertThat(ContentType("foo/bar.baz+zip").structure(), equalTo("zip"))
        assertThat(ContentType("foo/bar.baz+json", listOf("x" to "y")).structure(), equalTo("json"))
    }
}
