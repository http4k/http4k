package board

import board.domain.BoardId
import board.domain.CardText
import board.domain.Column
import board.domain.Role.Editor
import board.domain.Role.Viewer
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val plans = BoardId.of("plans")
private val bobsCard = CardText.of("Bobs card")
private fun columns(todo: List<CardText> = emptyList(), doing: List<CardText> = emptyList(), done: List<CardText> = emptyList()) =
    mapOf(Column.Todo to todo, Column.Doing to doing, Column.Done to done)

interface RolesContract {
    val driver: BoardDriver

    @Test
    fun `the first to open a board edits it, and later arrivals can only watch`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)

        assertThat(bob.adds(plans, "Sneaky card"), equalTo(Verdict.Denied))

        eventually { assertThat(ann.sees(plans).canEdit, equalTo(true)) }
        eventually { assertThat(bob.sees(plans).canEdit, equalTo(false)) }
        eventually { assertThat(ann.sees(plans).columns, equalTo(columns())) }
    }

    @Test
    fun `promoting a watcher to editor lets them edit, live`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        bob.watches(plans)

        ann.makesEditor(plans, bob)

        eventually { assertThat(bob.sees(plans).canEdit, equalTo(true)) }
        bob.adds(plans, bobsCard.value)
        eventually { assertThat(ann.sees(plans).columns, equalTo(columns(todo = listOf(bobsCard)))) }
    }

    @Test
    fun `everyone watching sees who is on the board and their role`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        ann.watches(plans)
        bob.watches(plans)

        eventually { assertThat(ann.sees(plans).members, equalTo(mapOf(ann.name to Editor, bob.name to Viewer))) }
    }
}
