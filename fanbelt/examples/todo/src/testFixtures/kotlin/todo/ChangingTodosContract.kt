package todo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import todo.domain.TodoTitle

private val buyMilk = TodoTitle.of("Buy milk")
private val walkTheDog = TodoTitle.of("Walk the dog")
private val buyOatMilk = TodoTitle.of("Buy oat milk")

interface ChangingTodosContract {
    val driver: TodoDriver

    @Test
    fun `toggling a todo marks it done and counts down what is left, live`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.adds(buyMilk)
        ann.adds(walkTheDog)
        bob.watches()

        ann.toggles(buyMilk)

        eventually { assertThat(bob.sees(), equalTo(ListSeen(listOf(buyMilk.value to true, walkTheDog.value to false), 1))) }
    }

    @Test
    fun `renaming to a blank title is rejected and the title is unchanged for everyone`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.adds(buyMilk)
        bob.watches()

        assertThat(ann.renames(buyMilk, "  "), equalTo(Verdict.Rejected("Title can't be blank or longer than 200 characters")))
        eventually { assertThat(bob.sees().todos, equalTo(listOf(buyMilk.value to false))) }

        ann.renames(buyMilk, buyOatMilk)
        eventually { assertThat(bob.sees().todos, equalTo(listOf(buyOatMilk.value to false))) }
    }

    @Test
    fun `deleting a todo removes it for everyone`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        ann.adds(buyMilk)
        bob.watches()

        ann.deletes(buyMilk)

        eventually { assertThat(bob.sees(), equalTo(ListSeen(emptyList(), 0))) }
    }
}
