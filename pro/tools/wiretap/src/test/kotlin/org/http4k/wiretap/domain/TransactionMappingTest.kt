package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.wiretap.util.formatBody
import org.junit.jupiter.api.Test

class TransactionMappingTest {

    @Test
    fun `prettifies JSON body`() {
        assertThat(
            formatBody("""{"a":1,"b":2}""", "application/json"),
            containsSubstring("\"a\": 1")
        )
    }

    @Test
    fun `prettifies XML body`() {
        assertThat(
            formatBody("<root><child>text</child></root>", "application/xml"),
            containsSubstring("  <child>text</child>")
        )
    }

    @Test
    fun `prettifies HTML body`() {
        assertThat(
            formatBody("<html><body><p>hello</p></body></html>", "text/html"),
            containsSubstring("  <body>")
        )
    }

    @Test
    fun `returns raw body for plain text`() {
        assertThat(
            formatBody("just plain text", "text/plain"),
            equalTo("just plain text")
        )
    }

    @Test
    fun `returns raw body when content type is empty`() {
        assertThat(
            formatBody("no content type", ""),
            equalTo("no content type")
        )
    }

    @Test
    fun `returns raw body for invalid JSON`() {
        assertThat(
            formatBody("not json", "application/json"),
            equalTo("not json")
        )
    }

    @Test
    fun `returns raw body for invalid XML`() {
        assertThat(
            formatBody("not xml", "application/xml"),
            equalTo("not xml")
        )
    }

    @Test
    fun `returns blank body unchanged`() {
        assertThat(
            formatBody("", "application/json"),
            equalTo("")
        )
    }

    @Test
    fun `handles json subtypes`() {
        assertThat(
            formatBody("""{"a":1}""", "application/ld+json"),
            containsSubstring("\"a\": 1")
        )
    }

    @Test
    fun `handles xml subtypes`() {
        assertThat(
            formatBody("<root><child/></root>", "application/soap+xml"),
            containsSubstring("  <child/>")
        )
    }
}
