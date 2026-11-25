package sheets

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Test
import sheets.domain.CellRef

private val a1 = CellRef.of("A1")
private val b5 = CellRef.of("B5")
private val c7 = CellRef.of("C7")

interface CursorsContract {
    val driver: SheetDriver

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
