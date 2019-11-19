package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ContentTypeTest {

    private val contentType0 = ContentType("none")
    private val contentType1 = ContentType("foo", "bob" to "bill")
    private val contentType2 = ContentType("foo", "bob" to "jim")
    private val contentType3 = ContentType("bar", "bob" to "jim")

    @Test
    fun `compare content types without directive`() {
        assertThat(contentType0.equalsIgnoringDirective(contentType0), equalTo(true))
        assertThat(contentType1.equalsIgnoringDirective(contentType1), equalTo(true))
        assertThat(contentType1.equalsIgnoringDirective(contentType2), equalTo(true))
        assertThat(contentType1.equalsIgnoringDirective(contentType3), equalTo(false))
    }

    @Test
    fun `directive to header value`() {
        assertThat(contentType0.toHeaderValue(), equalTo("none"))
        assertThat(contentType1.toHeaderValue(), equalTo("foo; bob=bill"))
    }

}