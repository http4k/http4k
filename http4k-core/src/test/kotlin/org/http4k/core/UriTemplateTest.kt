package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.UriTemplate.Companion.from
import org.junit.Test

class UriTemplateTest {

    @Test
    @Throws(Exception::class)
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
    fun canCaptureEnd() {
        val template1 = from("path")
        assertThat(template1.matches("path/123"), equalTo(true))
        assertThat(template1.extract("path/123").getValue("$"), equalTo("/123"))
        val template = from("path/{id}")
        assertThat(template.matches("path/123/someotherpath"), equalTo(true))
        assertThat(template.extract("path/123/someotherpath").getValue("$"), equalTo("/someotherpath"))
        assertThat(template.matches("path/123"), equalTo(true))
        assertThat(template.generate(pathParameters(pair("id", "123"), pair("$", "/someotherpath"))), equalTo("path/123/someotherpath"))
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

    fun pathParameters(vararg pairs: Pair<String, String>): Map<String, String> = mapOf(*pairs)

    fun pair(v1: String, v2: String) = v1 to v2
}

