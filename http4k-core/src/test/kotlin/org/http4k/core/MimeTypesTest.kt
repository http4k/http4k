package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MimeTypesTest {

    private val standardTypes = MimeTypes()

    @Test
    fun `uses known content types from mime types file`() {
        assertCorrectContentTypeFoundFor(standardTypes, "/foo/bob.xml", APPLICATION_XML.withNoDirectives())
        assertCorrectContentTypeFoundFor(standardTypes, "/foo/bob.html", TEXT_HTML.withNoDirectives())
        assertCorrectContentTypeFoundFor(standardTypes, "/foo/bob.txt", TEXT_PLAIN.withNoDirectives())
    }

    @Test
    fun `defaults back to octet stream for unknown file type`() {
        assertCorrectContentTypeFoundFor(standardTypes, "txt", OCTET_STREAM.withNoDirectives())
        assertCorrectContentTypeFoundFor(standardTypes, "/foo/bob.foobar", OCTET_STREAM.withNoDirectives())
    }

    @Test
    fun `reuses standard types`() {
        assertTrue(standardTypes === MimeTypes())
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
