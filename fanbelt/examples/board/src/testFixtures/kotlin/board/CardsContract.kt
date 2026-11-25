package board

import board.domain.BoardId
import board.domain.CardText
import board.domain.Column
import board.domain.Direction.Right
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val plans = BoardId.of("plans")
private val design = CardText.of("Design")
private fun columns(todo: List<CardText> = emptyList(), doing: List<CardText> = emptyList(), done: List<CardText> = emptyList()) =
    mapOf(Column.Todo to todo, Column.Doing to doing, Column.Done to done)

interface CardsContract {
    val driver: BoardDriver

    @Test
    fun `cards added by an editor appear live for everyone watching`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        bob.watches(plans)

        ann.adds(plans, design.value)

        eventually { assertThat(bob.sees(plans).columns, equalTo(columns(todo = listOf(design)))) }
    }

    @Test
    fun `a blank card is rejected for the sender`() {
        val ann = driver.member("ann")
        ann.opens(plans)

        assertThat(ann.adds(plans, "  "), equalTo(Verdict.Rejected("Card text can't be blank or longer than 100 characters")))
    }

    @Test
    fun `moving a card right walks it through the columns`() {
        val ann = driver.member("ann")
        ann.opens(plans)
        ann.adds(plans, design.value)

        ann.moves(plans, design, Right)
        eventually { assertThat(ann.sees(plans).columns, equalTo(columns(doing = listOf(design)))) }

        ann.moves(plans, design, Right)
        eventually { assertThat(ann.sees(plans).columns, equalTo(columns(done = listOf(design)))) }
    }

    @Test
    fun `deleting a card removes it for everyone`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        ann.adds(plans, design.value)
        bob.opens(plans)
        bob.watches(plans)

        ann.deletes(plans, design)

        eventually { assertThat(bob.sees(plans).columns, equalTo(columns())) }
    }
}
