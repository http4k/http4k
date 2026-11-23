package sheets

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
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

        assertThat(ann.sets(b5, rent.value), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.sees(b5), equalTo(rent)) }
    }

    @Test
    fun `clearing a cell empties it for everyone`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()
        ann.sets(c3, "1200")

        ann.sets(c3, empty.value)

        eventually { assertThat(bob.sees(c3), equalTo(empty)) }
    }

    @Test
    fun `a value that's too long is refused for the sender, and nothing changes for anyone else`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        assertThat(ann.sets(a1, "x".repeat(201)), equalTo(Verdict.Rejected("A cell holds up to 200 characters")))

        eventually { assertThat(bob.sees(a1), equalTo(empty)) }
    }

    @Test
    fun `everyone watching sees who's here`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()

        eventually { assertThat(bob.here(), equalTo(setOf(ann.name, bob.name))) }
    }

    @Test
    fun `a selected cell shows that viewer's cursor to everyone else, live, and it follows them`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()

        ann.selects(b5)
        eventually { assertThat(bob.cursors(), equalTo(mapOf(b5 to setOf(ann.name)))) }

        ann.selects(c7)
        eventually { assertThat(bob.cursors(), equalTo(mapOf(c7 to setOf(ann.name)))) }
    }

    @Test
    fun `everyone starts on A1, and everyone sharing a cell is shown there`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        val sam = driver.user("sam")
        ann.watches()
        bob.watches()
        sam.watches()

        eventually { assertThat(bob.cursors(), equalTo(mapOf(a1 to setOf(ann.name, sam.name)))) }
        eventually { assertThat(sam.cursors(), equalTo(mapOf(a1 to setOf(ann.name, bob.name)))) }
    }

    @Test
    fun `a viewer's cursor vanishes when they leave`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.watches()
        bob.watches()
        ann.selects(b5)

        ann.leaves()

        eventually(untilAClosedTabIsNoticed) { assertThat(bob.cursors(), equalTo(emptyMap<CellRef, Set<String>>())) }
        eventually { assertThat(bob.here(), equalTo(setOf(bob.name))) }
    }
}