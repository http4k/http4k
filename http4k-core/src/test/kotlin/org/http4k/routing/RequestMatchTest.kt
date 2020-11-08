package org.http4k.routing

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RequestMatchTest {

    @Test
    fun `query match`() {
        val query = queries("a", "b", "c")

        assertFalse(query((Request(GET, ""))))
        assertFalse(query(Request(GET, "").query("a", "value").query("b", "value")))
        assertFalse(query(Request(GET, "").header("a", "value").query("b", "value")))
        assertTrue(query((Request(GET, "").query("a", "value").query("b", "value").query("c", "value"))))
    }

    @Test
    fun `header match`() {
        val header = headers("a", "b", "c")

        assertFalse(header((Request(GET, ""))))
        assertFalse(header(Request(GET, "").header("a", "value").header("b", "value")))
        assertFalse(header(Request(GET, "").query("a", "value").header("b", "value")))
        assertTrue(header((Request(GET, "").header("a", "value").header("b", "value").header("c", "value"))))
    }
}
