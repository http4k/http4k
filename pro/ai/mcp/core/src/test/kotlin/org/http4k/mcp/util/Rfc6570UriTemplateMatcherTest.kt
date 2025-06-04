package org.http4k.mcp.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.mcp.model.ResourceUriTemplate
import org.http4k.mcp.util.Rfc6570UriTemplateMatcher.matches
import org.junit.jupiter.api.Test

class Rfc6570UriTemplateMatcherTest {

    @Test
    fun `template matches tests Rfc6570 basic cases`() {
        // Level 1 - Simple String Expansion
        assertMatch("http://localhost/{path}", "http://localhost/foobar")
        assertMatch("http://localhost:8080/{path}", "http://localhost:8080/foobar")
        assertMatch("http://{path}/{path}", "http://localhost:8080/foobar")
        assertMatch("http://{path}", "http://localhost:8080")
        assertMatch("http://{path}", "http://localhost")
        assertMatch("http://{path}/bar", "http://localhost/bar")
        assertMatch("http://{path}/bar", "http://localhost:8080/bar")

        // Original test cases for non-matching
        assertNoMatch("http://localhost:8080/{path}", "out")
        assertNoMatch("http://localhost/{path}", "nothttp://localhost/foobar")
        assertMatch("http://localhost:8080/{path}", "http://localhost:8080/")
    }

    @Test
    fun `template matches tests Rfc6570 level 1 templates`() {
        // Simple variable expansion
        assertMatch("http://example.com/{var}", "http://example.com/value")
        assertMatch("http://example.com/{hello}", "http://example.com/Hello%20World%21")

        // Multiple variables in path
        assertMatch("/search/{query}/page/{page}", "/search/cats/page/5")

        // With query parameters
        assertMatch("/users/{userId}/profile", "/users/123/profile")

        // Variable with special characters
        assertMatch("/document/{docId}", "/document/report-2024.pdf")
    }

    @Test
    fun `template matches tests Rfc6570 level 2 templates`() {
        // Reserved expansion with + operator
        assertMatch("http://example.com/{+path}/here", "http://example.com//foo/bar/here")
        assertMatch("http://example.com/{+path}", "http://example.com/foo/bar/here")
        assertMatch("here?ref={+path}", "here?ref=/foo/bar")

        // Fragment expansion with # operator
        assertMatch("http://example.com/document#{#fragment}", "http://example.com/document#section-3")
        assertMatch("http://example.com/{var}#{#fragment}", "http://example.com/value#section-3")
    }

    @Test
    fun `template matches tests with real-world examples`() {
        // API endpoints
        assertMatch("/api/v1/users/{userId}", "/api/v1/users/12345")
        assertMatch("/api/v1/users/{userId}/posts/{postId}", "/api/v1/users/12345/posts/6789")

        // Pagination
        assertMatch("/products{?page,limit}", "/products?page=2&limit=20")

        // Search with multiple parameters
        assertMatch("/search{?q,category,sort}", "/search?q=phone&category=electronics&sort=price-asc")

        // Domain with subdomains
        assertMatch("https://{subdomain}.example.com", "https://api.example.com")

        // REST-style paths
        assertMatch("/api/{resource}/{id}/{action}", "/api/products/1234/reviews")
    }

    @Test
    fun `template matches tests with edge cases`() {
        // Empty path segments
        assertMatch("http://example.com/{empty}/test", "http://example.com//test")

        // Multiple consecutive variables
        assertMatch("https://{host}/{path}", "https://example.com/index.html")

        // URL with port
        assertMatch("https://{host}:{port}/{path}", "https://example.com:8080/index.html")

        // Query parameters with special characters
        assertMatch("/search{?q}", "/search?q=special%20chars%21")

        // Path with numbers
        assertMatch("/v{version}/api", "/v2/api")

        // Paths with extensions
        assertMatch("/files/{filename}.{ext}", "/files/document.pdf")
    }

    @Test
    fun `template expansion with level 1 templates`() {
        // Level 1 - Simple String Expansion
        assertExpansion(
            template = "{var}",
            variables = mapOf("var" to "value"),
            expected = "value"
        )

        assertExpansion(
            template = "{hello}",
            variables = mapOf("hello" to "Hello World!"),
            expected = "Hello%20World%21"
        )

        assertExpansion(
            template = "{half}",
            variables = mapOf("half" to "50%"),
            expected = "50%25"
        )

        assertExpansion(
            template = "O{empty}X",
            variables = mapOf("empty" to ""),
            expected = "OX"
        )

        assertExpansion(
            template = "O{undef}X",
            variables = mapOf("other" to "value"),
            expected = "OX"
        )

        assertExpansion(
            template = "{x,y}",
            variables = mapOf("x" to "1024", "y" to "768"),
            expected = "1024,768"
        )

        assertExpansion(
            template = "{x,hello,y}",
            variables = mapOf("x" to "1024", "hello" to "Hello World!", "y" to "768"),
            expected = "1024,Hello%20World%21,768"
        )
    }

    @Test
    fun `template expansion with level 2 templates`() {
        // Level 2 - Reserved expansion with + operator
        assertExpansion(
            template = "{+var}",
            variables = mapOf("var" to "value"),
            expected = "value"
        )

        assertExpansion(
            template = "{+hello}",
            variables = mapOf("hello" to "Hello World!"),
            expected = "Hello%20World!"
        )

        assertExpansion(
            template = "{+path}/here",
            variables = mapOf("path" to "/foo/bar"),
            expected = "/foo/bar/here"
        )

        assertExpansion(
            template = "here?ref={+path}",
            variables = mapOf("path" to "/foo/bar"),
            expected = "here?ref=/foo/bar"
        )

        // Level 2 - Fragment expansion with # operator
        assertExpansion(
            template = "{#var}",
            variables = mapOf("var" to "value"),
            expected = "#value"
        )

        assertExpansion(
            template = "{#hello}",
            variables = mapOf("hello" to "Hello World!"),
            expected = "#Hello%20World!"
        )
    }

    private fun assertMatch(input: String, value: String) {
        assertThat(
            "Template '$input' should match '$value'",
            ResourceUriTemplate.of(input).matches(Uri.of(value)), equalTo(true)
        )
    }

    private fun assertNoMatch(input: String, value: String) {
        assertThat(
            "Template '$input' should NOT match '$value'",
            ResourceUriTemplate.of(input).matches(Uri.of(value)), equalTo(false)
        )
    }

    private fun assertExpansion(template: String, variables: Map<String, Any>, expected: String) {
        assertThat(
            "Template '$template' expansion with $variables",
            Rfc6570UriTemplateMatcher.expand(template, variables), equalTo(expected)
        )
    }
}
