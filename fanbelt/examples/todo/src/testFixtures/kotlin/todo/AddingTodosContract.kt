package todo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import todo.domain.TodoTitle

private val buyMilk = TodoTitle.of("Buy milk")
private val walkTheDog = TodoTitle.of("Walk the dog")

interface AddingTodosContract {
    val driver: TodoDriver

    @Test
    fun `a todo added by one user appears live for everyone watching`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        ann.adds(buyMilk)
        ann.adds(walkTheDog)

        eventually { assertThat(bob.sees(), equalTo(ListSeen(listOf(buyMilk.value to false, walkTheDog.value to false), 2))) }
    }

    @Test
    fun `a blank title is rejected for the sender, and nobody else sees anything`() {
        val ann = driver.user("ann")
        val bob = driver.user("bob")
        bob.watches()

        assertThat(ann.adds("   "), equalTo(Verdict.Rejected("Title can't be blank or longer than 200 characters")))
        eventually { assertThat(bob.sees(), equalTo(ListSeen(emptyList(), 0))) }
    }
}
