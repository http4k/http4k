package org.http4k.core

import org.http4k.core.AcceptableOperationsTest.PossibleValues.a
import org.http4k.core.AcceptableOperationsTest.PossibleValues.b
import org.http4k.core.AcceptableOperationsTest.PossibleValues.c
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AcceptableOperationsTest {
    enum class PossibleValues { a, b, c }

    // The tests shuffle the lists of offered options to show that the selection
    // is driven by the quality parameter of the client's preferences, not the
    // order in which options are offered by the server.

    @Test
    fun `selects client's preferred value from available list`() {
        val prefs = PriorityList(a q 1.0, b q 0.75, c q 0.5)

        assertEquals(a, prefs.preferred(listOf(a), match = Any::equals, by = { it }))
        assertEquals(b, prefs.preferred(listOf(b), match = Any::equals, by = { it }))
        assertEquals(c, prefs.preferred(listOf(c), match = Any::equals, by = { it }))
        assertEquals(a, prefs.preferred(listOf(a, b).shuffled(), match = Any::equals, by = { it }))
        assertEquals(b, prefs.preferred(listOf(b, c).shuffled(), match = Any::equals, by = { it }))
        assertEquals(a, prefs.preferred(listOf(a, c).shuffled(), match = Any::equals, by = { it }))
        assertEquals(a, prefs.preferred(listOf(a, b, c).shuffled(), match = Any::equals, by = { it }))
    }

    @Test
    fun `honours wildcards`() {
        val prefs = PriorityList(
            Exactly(a) q 1.0,
            Exactly(b) q 0.75,
            Wildcard q 0.5)

        assertEquals(a, prefs.preferred(listOf(a)))
        assertEquals(b, prefs.preferred(listOf(b)))
        assertEquals(c, prefs.preferred(listOf(c)))
        assertEquals(a, prefs.preferred(listOf(a, b).shuffled()))
        assertEquals(b, prefs.preferred(listOf(b, c).shuffled()))
        assertEquals(a, prefs.preferred(listOf(a, c).shuffled()))
        assertEquals(a, prefs.preferred(listOf(a, b, c).shuffled()))
    }

    @Test
    fun `never selects an option the client rejects with zero quality`() {
        val prefs = PriorityList(a q 1.0, b q 0.0)

        assertEquals(null, prefs.preferred(listOf(b), match = Any::equals, by = { it }))
        assertEquals(a, prefs.preferred(listOf(b, a), match = Any::equals, by = { it }))
    }

    @Test
    fun `explicit preference overrides wildcard`() {
        val prefs = PriorityList(Wildcard q 1.0, Exactly(a) q 0.0, Exactly(b) q 0.5)

        assertEquals(null, prefs.preferred(listOf(a)))
        assertEquals(c, prefs.preferred(listOf(a, b, c)))
        assertEquals(b, prefs.preferred(listOf(a, b)))
    }

    @Test
    fun `selects in server order when client preferences are equal`() {
        val prefs = PriorityList(a q 1.0, b q 1.0, c q 1.0)

        assertEquals(c, prefs.preferred(listOf(c, b, a), match = Any::equals, by = { it }))
        assertEquals(b, prefs.preferred(listOf(b, a), match = Any::equals, by = { it }))
    }
}
