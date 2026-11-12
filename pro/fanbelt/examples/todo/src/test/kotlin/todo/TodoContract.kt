package todo

import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import todo.domain.TodoTitle

private val buyMilk = TodoTitle.of("Buy milk")
private val walkTheDog = TodoTitle.of("Walk the dog")
private val buyOatMilk = TodoTitle.of("Buy oat milk")

abstract class TodoContract {
    abstract val driver: TodoDriver

    @Test
    fun `a todo added by one user appears live for everyone watching`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        ann.adds(buyMilk)
        ann.adds(walkTheDog)

        eventually { assertEquals(ListSeen(listOf(buyMilk.value to false, walkTheDog.value to false), 2), bob.sees()) }
    }

    @Test
    fun `a blank title is rejected for the sender, and nobody else sees anything`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        assertEquals(Verdict.Rejected("Title can't be blank or longer than 200 characters"), ann.adds("   "))
        eventually { assertEquals(ListSeen(emptyList(), 0), bob.sees()) }
    }

    @Test
    fun `toggling a todo marks it done and counts down what is left, live`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.adds(buyMilk)
        ann.adds(walkTheDog)
        bob.watches()

        ann.toggles(buyMilk)

        eventually { assertEquals(ListSeen(listOf(buyMilk.value to true, walkTheDog.value to false), 1), bob.sees()) }
    }

    @Test
    fun `renaming to a blank title is rejected and the title is unchanged for everyone`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.adds(buyMilk)
        bob.watches()

        assertEquals(Verdict.Rejected("Title can't be blank or longer than 200 characters"), ann.renames(buyMilk, "  "))
        eventually { assertEquals(listOf(buyMilk.value to false), bob.sees().todos) }

        ann.renames(buyMilk, buyOatMilk)
        eventually { assertEquals(listOf(buyOatMilk.value to false), bob.sees().todos) }
    }

    @Test
    fun `deleting a todo removes it for everyone`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.adds(buyMilk)
        bob.watches()

        ann.deletes(buyMilk)

        eventually { assertEquals(ListSeen(emptyList(), 0), bob.sees()) }
    }
}