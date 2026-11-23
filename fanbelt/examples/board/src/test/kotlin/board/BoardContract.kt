package board

import board.domain.BoardId
import board.domain.BuildStatus
import board.domain.CardText
import board.domain.Column
import board.domain.Commit
import board.domain.Direction.Right
import board.domain.Role.Editor
import board.domain.Role.Viewer
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val plans = BoardId.of("plans")
private val design = CardText.of("Design")
private val bobsCard = CardText.of("Bobs card")
private val passingCommit = Commit.of("abc1234")
private val failingCommit = Commit.of("def5678")

abstract class BoardContract {
    abstract val driver: BoardDriver

    private fun columns(todo: List<CardText> = emptyList(), doing: List<CardText> = emptyList(), done: List<CardText> = emptyList()) =
        mapOf(Column.Todo to todo, Column.Doing to doing, Column.Done to done)

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

    @Test
    fun `builds reported by CI appear live for everyone watching, newest first`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        bob.watches(plans)

        assertThat(driver.ciReports(plans, "passing", passingCommit.value), equalTo(Verdict.Accepted))
        driver.ciReports(plans, "failing", failingCommit.value)

        eventually { assertThat(bob.sees(plans).builds, equalTo(listOf(BuildSeen(BuildStatus.Failing, failingCommit), BuildSeen(BuildStatus.Passing, passingCommit)))) }
    }

    @Test
    fun `a build report CI can't read is refused with every reason`() {
        assertThat(driver.ciReports(plans, "exploded", "nope"), equalTo(Verdict.Rejected("A build is passing, failing or running", "A commit is 7 to 40 hex characters")))
    }
}