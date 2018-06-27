package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.junit.jupiter.api.Test

class MimeTypesTest {

    @Test
    fun `uses known content types from mime types file`() {
        assertCorrectContentTypeFoundFor(MimeTypes(), "/foo/bob.xml", APPLICATION_XML.withNoDirective())
        assertCorrectContentTypeFoundFor(MimeTypes(), "/foo/bob.html", TEXT_HTML.withNoDirective())
        assertCorrectContentTypeFoundFor(MimeTypes(), "/foo/bob.txt", TEXT_PLAIN.withNoDirective())
    }

    @Test
    fun `defaults back to octet stream for unknown file type`() {
        assertCorrectContentTypeFoundFor(MimeTypes(), "txt", OCTET_STREAM.withNoDirective())
        assertCorrectContentTypeFoundFor(MimeTypes(), "/foo/bob.foobar", OCTET_STREAM.withNoDirective())
    }

    @Test
    fun `can override content type`() {
        assertCorrectContentTypeFoundFor(MimeTypes(mapOf("foobar" to TEXT_HTML)), "/foo/bob.foobar", TEXT_HTML)
    }

    private fun assertCorrectContentTypeFoundFor(mimeTypes: MimeTypes, ext: String, expected: ContentType) {
        assertThat("checking $ext", mimeTypes.forFile(ext), equalTo(expected))
        assertThat("checking ${ext.toUpperCase()}", mimeTypes.forFile(ext.toUpperCase()), equalTo(expected))
    }
}