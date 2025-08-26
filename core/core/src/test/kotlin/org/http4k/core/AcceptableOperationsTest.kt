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
        
        assertEquals(a, prefs.preferredOf(listOf(a), match = Any::equals))
        assertEquals(b, prefs.preferredOf(listOf(b), match = Any::equals))
        assertEquals(c, prefs.preferredOf(listOf(c), match = Any::equals))
        assertEquals(a, prefs.preferredOf(listOf(a, b).shuffled(), match = Any::equals))
        assertEquals(b, prefs.preferredOf(listOf(b, c).shuffled(), match = Any::equals))
        assertEquals(a, prefs.preferredOf(listOf(a, c).shuffled(), match = Any::equals))
        assertEquals(a, prefs.preferredOf(listOf(a, b, c).shuffled(), match = Any::equals))
    }
    
    @Test
    fun `honours wildcards`() {
        val prefs = PriorityList(
            Exactly(a) q 1.0,
            Exactly(b) q 0.75,
            Wildcard q 0.5)
        
        assertEquals(a, prefs.preferredOf(listOf(a)))
        assertEquals(b, prefs.preferredOf(listOf(b)))
        assertEquals(c, prefs.preferredOf(listOf(c)))
        assertEquals(a, prefs.preferredOf(listOf(a, b).shuffled()))
        assertEquals(b, prefs.preferredOf(listOf(b, c).shuffled()))
        assertEquals(a, prefs.preferredOf(listOf(a, c).shuffled()))
        assertEquals(a, prefs.preferredOf(listOf(a, b, c).shuffled()))
    }
}
