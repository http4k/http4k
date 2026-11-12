package sheets

import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sheets.domain.CellRef
import sheets.domain.CellText

abstract class SheetContract {
    abstract val driver: SheetDriver

    private val a1 = CellRef.of("A1")
    private val b5 = CellRef.of("B5")
    private val c3 = CellRef.of("C3")
    private val c7 = CellRef.of("C7")
    private val rent = CellText.of("Rent")
    private val empty = CellText.of("")

    @Test
    fun `a value set by one user appears live for everyone watching`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        assertEquals(Verdict.Accepted, ann.sets(b5, rent.value))

        eventually { assertEquals(rent, bob.sees(b5)) }
    }

    @Test
    fun `clearing a cell empties it for everyone`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()
        ann.sets(c3, "1200")

        ann.sets(c3, empty.value)

        eventually { assertEquals(empty, bob.sees(c3)) }
    }

    @Test
    fun `a value that's too long is refused for the sender, and nothing changes for anyone else`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        assertEquals(Verdict.Rejected("A cell holds up to 200 characters"), ann.sets(a1, "x".repeat(201)))

        eventually { assertEquals(empty, bob.sees(a1)) }
    }

    @Test
    fun `everyone watching sees who's here`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()

        eventually { assertEquals(setOf(ann.name, bob.name), bob.here()) }
    }

    @Test
    fun `a selected cell shows that viewer's cursor to everyone else, live, and it follows them`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()

        ann.selects(b5)
        eventually { assertEquals(mapOf(b5 to setOf(ann.name)), bob.cursors()) }

        ann.selects(c7)
        eventually { assertEquals(mapOf(c7 to setOf(ann.name)), bob.cursors()) }
    }

    @Test
    fun `everyone starts on A1, and everyone sharing a cell is shown there`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        val sam = driver.user("sam")
        ann.watches()
        bob.watches()
        sam.watches()

        eventually { assertEquals(mapOf(a1 to setOf(ann.name, sam.name)), bob.cursors()) }
        eventually { assertEquals(mapOf(a1 to setOf(ann.name, bob.name)), sam.cursors()) }
    }

    @Test
    fun `a viewer's cursor vanishes when they leave`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()
        ann.selects(b5)

        ann.leaves()

        eventually(untilAClosedTabIsNoticed) { assertEquals(emptyMap<CellRef, Set<String>>(), bob.cursors()) }
        eventually { assertEquals(setOf(bob.name), bob.here()) }
    }
}