package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class UriTest {
    @Test
    fun `parsing a full uri`() {
        val value = "http://user:pass@host:1234/some/path?q1=v1&q2=v2#abc"
        val uri = Uri.of(value)
        assertThat(uri.scheme, equalTo("http"))
        assertThat(uri.authority, equalTo("user:pass@host:1234"))
        assertThat(uri.host, equalTo("host"))
        assertThat(uri.port, equalTo(1234))
        assertThat(uri.fragment, equalTo("abc"))
        assertThat(uri.path, equalTo("/some/path"))
        assertThat(uri.query, equalTo("q1=v1&q2=v2"))
        assertThat(uri.toString(), equalTo(value))
    }

    @Test
    fun `creating a full uri by hand`() {
        val uri = Uri.of("")
            .scheme("https")
            .userInfo("user:pass")
            .host("example.com")
            .port(1234)
            .fragment("bob")
            .path("/a/b/c")
            .query("foo=bar")

        assertThat(uri.toString(), equalTo("https://user:pass@example.com:1234/a/b/c?foo=bar#bob"))
    }

    @Test
    fun `handles no prefixed slash in a path`() {
        val uri = Uri.of("")
            .scheme("https")
            .host("example.com")
            .port(1234)
            .path("a/b/c")

        assertThat(uri.toString(), equalTo("https://example.com:1234/a/b/c"))
    }

    @Test
    fun `equality for round tripping`() {
        assertThat(Uri.of("path"), equalTo(Uri.of(Uri.of("path").toString())))
    }

    @Test
    fun can_parse_minimal_uri() {
        val value = "http://host"
        val uri = Uri.of(value)
        assertThat(uri.scheme, equalTo("http"))
        assertThat(uri.host, equalTo("host"))
        assertThat(uri.authority, equalTo("host"))
        assertThat(uri.userInfo, equalTo(""))
        assertNull(uri.port)
        assertThat(uri.fragment, equalTo(""))
        assertThat(uri.path, equalTo(""))
        assertThat(uri.query, equalTo(""))
        assertThat(uri.toString(), equalTo(value))
    }

    @Test
    fun handles_empty_uri() {
        assertThat(Uri.of("").toString(), equalTo(""))
    }

    @Test
    fun can_add_parameter() {
        assertThat(Uri.of(value = "http://ignore").query("a", "b").toString(), equalTo("http://ignore?a=b"))
    }

    @Test
    fun can_remove_parameter() {
        assertThat(
            Uri.of(value = "http://ignore")
                .query("a", "b")
                .query("c", "d")
                .query("a", "c")
                .removeQuery("a").toString(), equalTo("http://ignore?c=d")
        )
    }

    @Test
    fun can_remove_all_parameter_with_prefix() {
        assertThat(
            Uri.of(value = "http://ignore")
                .query("aa", "b")
                .query("c", "d")
                .query("ab", "c")
                .removeQueries("a").toString(), equalTo("http://ignore?c=d")
        )
    }

    @Test
    fun can_remove_all_parameter() {
        assertThat(
            Uri.of(value = "http://ignore")
                .query("aa", "b")
                .query("c", "d")
                .query("ab", "c")
                .removeQueries().toString(), equalTo("http://ignore")
        )
    }

    @Test
    fun parameters_can_be_defined_in_value() {
        assertThat(Uri.of("http://www.google.com?a=b"), equalTo(Uri.of("http://www.google.com").query("a", "b")))
    }

    @Test
    fun `can encode a path segment correctly`() {
        val original = "123 / 456"
        val encoded = "123%20%2F%20456"
        assertThat(original.toPathSegmentEncoded(), equalTo(encoded))
    }

    @Test
    fun `can extend existing uri path`() {
        assertThat(Uri.of("http://ignore").extend(Uri.of("/")), equalTo(Uri.of("http://ignore/")))
        assertThat(Uri.of("http://ignore/a").extend(Uri.of("")), equalTo(Uri.of("http://ignore/a")))
        assertThat(Uri.of("http://ignore/a").extend(Uri.of("/b")), equalTo(Uri.of("http://ignore/a/b")))
        assertThat(Uri.of("http://ignore/a").extend(Uri.of("b")), equalTo(Uri.of("http://ignore/a/b")))
        assertThat(Uri.of("http://ignore/a").extend(Uri.of("b/")), equalTo(Uri.of("http://ignore/a/b/")))
    }

    @Test
    fun `can extend existing uri`() {
        assertThat(
            Uri.of("http://ignore?foo=bar").extend(Uri.of("/?abc=xyz#bob")),
            equalTo(Uri.of("http://ignore/?foo=bar&abc=xyz#bob"))
        )
        assertThat(
            Uri.of("http://ignore?foo=bar#bob").extend(Uri.of("/?abc=xyz")),
            equalTo(Uri.of("http://ignore/?foo=bar&abc=xyz#bob"))
        )
    }

    @Test
    fun `can encode query param values`() {
        val unEncodedUri = Uri.of("http://ignore/?q1=encode me pls&q2=encode me 2")
        val encodedUri = "http://ignore/?q1=encode+me+pls&q2=encode+me+2"
        val queryParametersEncodedUri = unEncodedUri.queryParametersEncoded()
        assertThat(queryParametersEncodedUri.toString(), equalTo(encodedUri))
        assertThat(queryParametersEncodedUri.queries(), equalTo(listOf("q1" to "encode me pls", "q2" to "encode me 2")))
    }

    // Test cases adapted from [reference resolution examples of RFC 3986](https://datatracker.ietf.org/doc/html/rfc3986#section-5.4)
    @ParameterizedTest(name = "base {0}, relative path {1}, maps to {2} ")
    @MethodSource("relativeUriTestData")
    fun `handles relative URIs`(baseUri: Uri, relativeUri: String, expected: Uri) {
        assertThat(baseUri.relative(relativeUri), equalTo(expected))
    }

    companion object {
        @JvmStatic
        fun relativeUriTestData(): List<Arguments> {
            return listOf(
                // [Normal Examples](https://datatracker.ietf.org/doc/html/rfc3986#section-5.4.1)
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "", Uri.of("http://a/b/c/d?q#f")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "http://e/f/g/h?i#j", Uri.of("http://e/f/g/h?i#j")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g:h", Uri.of("g:h")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g", Uri.of("http://a/b/c/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "./g", Uri.of("http://a/b/c/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g/", Uri.of("http://a/b/c/g/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "/g", Uri.of("http://a/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "//g", Uri.of("http://g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "?y", Uri.of("http://a/b/c/d?y")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g?y", Uri.of("http://a/b/c/g?y")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g?y/./x", Uri.of("http://a/b/c/g?y/./x")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "#s", Uri.of("http://a/b/c/d?q#s")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g#s", Uri.of("http://a/b/c/g#s")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g#s/./x", Uri.of("http://a/b/c/g#s/./x")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g?y#s", Uri.of("http://a/b/c/g?y#s")),
//                Arguments.of(Uri.of("http://a/b/c/d?q#f"), ";x", Uri.of("http://a/b/c/d;x")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g;x", Uri.of("http://a/b/c/g;x")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g;x?y#s", Uri.of("http://a/b/c/g;x?y#s")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), ".", Uri.of("http://a/b/c/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "./", Uri.of("http://a/b/c/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "..", Uri.of("http://a/b/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "../", Uri.of("http://a/b/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "../g", Uri.of("http://a/b/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "../..", Uri.of("http://a/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "../../", Uri.of("http://a/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "../../g", Uri.of("http://a/g")),

                // [Abnormal Examples](https://datatracker.ietf.org/doc/html/rfc3986#section-5.4.2)
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "../../../g", Uri.of("http://a/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "./../../../g", Uri.of("http://a/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "/./g", Uri.of("http://a/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "/../g", Uri.of("http://a/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g.", Uri.of("http://a/b/c/g.")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), ".g", Uri.of("http://a/b/c/.g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g..", Uri.of("http://a/b/c/g..")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "..g", Uri.of("http://a/b/c/..g")),

                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "./../g", Uri.of("http://a/b/g")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "./g/.", Uri.of("http://a/b/c/g/")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g/./h", Uri.of("http://a/b/c/g/h")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g/../h", Uri.of("http://a/b/c/h")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), "g;x=1/./y", Uri.of("http://a/b/c/g;x=1/y")),
                Arguments.of(Uri.of("http://a/b/c/d?q#f"), ".g;x=1/../y", Uri.of("http://a/b/c/y")),
                )
        }
    }
}
