package features.grid

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val a1 = CellRef.of("A1")
private val b5 = CellRef.of("B5")
private val c3 = CellRef.of("C3")
private val rent = CellText.of("Rent")
private val empty = CellText.of("")

interface CellsContract {
    val driver: GridDriver

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
}
