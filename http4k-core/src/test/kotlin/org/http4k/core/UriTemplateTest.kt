package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.UriTemplate.Companion.from
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

class UriTemplateTest {

    @Test
    fun encodesOnlyPathParamsWhichDontContainForwardSlashes() {
        val template = from("properties/{name}")

        assertThat(
            template.generate(pathParameters(pair("name", "a name with spaces"))),
            equalTo("properties/a+name+with+spaces"))

        assertThat(
            template.generate(pathParameters(pair("name", "a/name/with/slashes"))),
            equalTo("properties/a/name/with/slashes"))
    }

    @Test
    fun supportsMultiplePathParams() {
        val template = from("properties/{id}/{name}")
        assertThat(template.matches("properties/123/bob"), equalTo(true))
        val parameters = template.extract("properties/123/bob")
        assertThat(parameters.getValue("id"), equalTo("123"))
        assertThat(parameters.getValue("name"), equalTo("bob"))
        assertThat(template.generate(pathParameters(pair("id", "123"), pair("name", "bob"))), equalTo("properties/123/bob"))
    }

    @Test
    fun doesNotCaptureEnd() {
        val template1 = from("path")
        assertThat(template1.matches("path/123"), equalTo(false))
        val template = from("path/{id}")
        assertThat(template.matches("path/123/someotherpath"), equalTo(false))
        assertThat(template.matches("path/123"), equalTo(true))
        assertThat(template.generate(pathParameters(pair("id", "123"))), equalTo("path/123"))
    }

    @Test
    fun supportsCustomRegex() {
        val template = from("path/{id:\\d}")
        assertThat(template.matches("path/foo"), equalTo(false))
        assertThat(template.matches("path/1"), equalTo(true))
        assertThat(template.extract("path/1").getValue("id"), equalTo("1"))
    }

    @Test
    fun canMatch() {
        assertThat(from("path/{id}").matches("path/foo"), equalTo(true))
        assertThat(from("/path/{id}").matches("/path/foo"), equalTo(true))
        assertThat(from("/path/{id}/").matches("/path/foo"), equalTo(true))
        assertThat(from("/path/{id}/").matches("path/foo"), equalTo(true))
        assertThat(from("path/{id}").matches("/path/foo"), equalTo(true))
    }

    @Test
    fun canExtractFromUri() {
        val template = from("path/{id}")
        assertThat(template.extract("path/foo").getValue("id"), equalTo("foo"))
    }

    @Test
    fun fallbackDoesNotWork() {
        val template = from("/b/c")
        assertThat(template.matches("/b/c/e/f"), equalTo(false))
    }

    @Test
    fun canExtractFromUri_withLeadingSlash() {
        val template = from("/{id:.+}/{id2:.+}")
        val extracted = template.extract("/foo/bar")
        assertThat(extracted.getValue("id"), equalTo("foo"))
        assertThat(extracted.getValue("id2"), equalTo("bar"))
    }

    @Test
    fun canExtractFromUri_withTrailingSlash() {
        val template = from("/{id:.+}/{id2:.+}/")
        val extracted = template.extract("/foo/bar/")
        assertThat(extracted.getValue("id"), equalTo("foo"))
        assertThat(extracted.getValue("id2"), equalTo("bar"))
        val extractedNoTrailing = template.extract("/foo/bar/")
        assertThat(extractedNoTrailing.getValue("id"), equalTo("foo"))
        assertThat(extractedNoTrailing.getValue("id2"), equalTo("bar"))
    }

    @Test
    fun canExtractFromUriWithEncodedSpace() {
        val template = from("path/{id1}")
        assertThat(template.extract("path/foo+bar").getValue("id1"), equalTo("foo bar"))
    }

    @Test
    fun canGenerateUri() {
        val template = from("path/{id}")
        assertThat(template.generate(pathParameters(pair("id", "foo"))), equalTo("path/foo"))
    }

    @Test
    fun doesNotDecodeSlashesWhenCapturing() {
        val extracted = from("path/{first}/{second}").extract("path/1%2F2/3")
        assertThat(extracted.getValue("first"), equalTo("1/2"))
        assertThat(extracted.getValue("second"), equalTo("3"))
    }

    @Test
    fun matchedValuesAreUrlDecoded() {
        val extracted = from("path/{band}").extract("path/Earth%2C%20Wind%20%26%20Fire")
        assertThat(extracted.getValue("band"), equalTo("Earth, Wind & Fire"))
    }

    @Test
    fun capturingPathVariableWithSlashes() {
        val template = from("/{anything:.*}")
        assertThat(template.matches("/foo/bar"), equalTo(true))
        assertThat(template.extract("/foo/bar").getValue("anything"), equalTo("foo/bar"))
    }

    @Test
    fun doesNotMatchPathWithSlashesForUnnamedVariable() {
        assertThat(from("/{:.*}").matches("/foo/bar"), equalTo(false))
        assertThat(from("/{something:.*}").matches("/foo/bar"), equalTo(true))
    }

    @Test
    fun doesNotMatchEmptyPathSegment() {
        assertThat(from("/foo/{bar:.*}").matches("/foo/bar"), equalTo(true))
        assertThat(from("/foo/{bar:.*}").matches("/foo"), equalTo(false))
    }

    @Test
    @Disabled
    fun greedyQualifiersAreNotReplaced() {
         val COMPACT_UUID_REGEX = "[a-fA-F0-9]{32}"
         val PRETTY_UUID_REGEX = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
         val UUID_REGEX = "(?:${COMPACT_UUID_REGEX}|${PRETTY_UUID_REGEX})"

        assertThat(from("/foo/{bar:$UUID_REGEX}").matches("/foo/${UUID.randomUUID()}"), equalTo(true))
    }

    private fun pathParameters(vararg pairs: Pair<String, String>): Map<String, String> = mapOf(*pairs)

    private fun pair(v1: String, v2: String) = v1 to v2
}
