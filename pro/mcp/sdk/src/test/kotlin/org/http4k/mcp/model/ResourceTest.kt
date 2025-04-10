package org.http4k.mcp.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test


class ResourceTest {

    @Test
    fun `template matches tests`() {
        assertMatch("http://localhost/{path}", "http://localhost/foobar")
        assertMatch("http://localhost:8080/{path}", "http://localhost:8080/foobar")
        assertMatch("http://{path}/{path}", "http://localhost:8080/foobar")

        assertMatch("http://{path}", "http://localhost:8080")
        assertMatch("http://{path}", "http://localhost")

        assertMatch("http://{path}/bar", "http://localhost/bar")
        assertMatch("http://{path}/bar", "http://localhost:8080/bar")

        assertNoMatch("http://localhost:8080/{path}", "out")
        assertNoMatch("http://localhost/{path}", "nothttp://localhost/foobar")
        assertNoMatch("http://localhost:8080/{path}", "http://localhost:8080/")
    }

    private fun assertMatch(input: String, value: String) {
        assertThat(matches(input, value), equalTo(true))
    }

    private fun assertNoMatch(input: String, value: String) {
        assertThat(matches(input, value), equalTo(false))
    }

    fun matches(input: String, value: String): Boolean {
        if (value.isEmpty()) return false

        val regex = input
            .replace(".", "\\.")
            .replace("?", "\\?")
            .replace("*", "\\*")
            .replace("+", "\\+")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("|", "\\|")
            .replace(Regex("\\{[^}]+}"), "([^/]+)")

        return Regex("^$regex$").matches(value)
    }
}
