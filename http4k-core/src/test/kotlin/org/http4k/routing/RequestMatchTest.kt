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

        assertFalse(query.match(Request(GET, "")).matched())
        assertFalse(query.match(Request(GET, "").query("a", "value").query("b", "value")).matched())
        assertFalse(query.match(Request(GET, "").header("a", "value").query("b", "value")).matched())
        assertTrue(query.match((Request(GET, "").query("a", "value").query("b", "value").query("c", "value"))).matched())
    }

    @Test
    fun `header match`() {
        val header = headers("a", "b", "c")

        assertFalse(header.match(Request(GET, "")).matched())
        assertFalse(header.match(Request(GET, "").header("a", "value").header("b", "value")).matched())
        assertFalse(header.match(Request(GET, "").query("a", "value").header("b", "value")).matched())
        assertTrue(header.match(Request(GET, "").header("a", "value").header("b", "value").header("c", "value")).matched())
    }
}

fun RouterMatch.matched() = (this == RouterMatch.MatchedWithoutHandler)
