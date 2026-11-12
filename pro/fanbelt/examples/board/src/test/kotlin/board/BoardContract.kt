package board

import board.domain.BoardId
import board.domain.BuildStatus
import board.domain.CardText
import board.domain.Column
import board.domain.Commit
import board.domain.Direction.Right
import board.domain.Role.Editor
import board.domain.Role.Viewer
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Assertions.assertEquals
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

        assertEquals(Verdict.Denied, bob.adds(plans, "Sneaky card"))

        eventually { assertEquals(true, ann.sees(plans).canEdit) }
        eventually { assertEquals(false, bob.sees(plans).canEdit) }
        eventually { assertEquals(columns(), ann.sees(plans).columns) }
    }

    @Test
    fun `cards added by an editor appear live for everyone watching`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        bob.watches(plans)

        ann.adds(plans, design.value)

        eventually { assertEquals(columns(todo = listOf(design)), bob.sees(plans).columns) }
    }

    @Test
    fun `a blank card is rejected for the sender`() {
        val ann = driver.member("ann")
        ann.opens(plans)

        assertEquals(Verdict.Rejected("Card text can't be blank or longer than 100 characters"), ann.adds(plans, "  "))
    }

    @Test
    fun `moving a card right walks it through the columns`() {
        val ann = driver.member("ann")
        ann.opens(plans)
        ann.adds(plans, design.value)

        ann.moves(plans, design, Right)
        eventually { assertEquals(columns(doing = listOf(design)), ann.sees(plans).columns) }

        ann.moves(plans, design, Right)
        eventually { assertEquals(columns(done = listOf(design)), ann.sees(plans).columns) }
    }

    @Test
    fun `promoting a watcher to editor lets them edit, live`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        bob.watches(plans)

        ann.makesEditor(plans, bob)

        eventually { assertEquals(true, bob.sees(plans).canEdit) }
        bob.adds(plans, bobsCard.value)
        eventually { assertEquals(columns(todo = listOf(bobsCard)), ann.sees(plans).columns) }
    }

    @Test
    fun `everyone watching sees who is on the board and their role`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        ann.watches(plans)
        bob.watches(plans)

        eventually { assertEquals(mapOf(ann.name to Editor, bob.name to Viewer), ann.sees(plans).members) }
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

        eventually { assertEquals(columns(), bob.sees(plans).columns) }
    }

    @Test
    fun `builds reported by CI appear live for everyone watching, newest first`() {
        val ann = driver.member("ann")
        val bob = driver.member("bob")
        ann.opens(plans)
        bob.opens(plans)
        bob.watches(plans)

        assertEquals(Verdict.Accepted, driver.ciReports(plans, "passing", passingCommit.value))
        driver.ciReports(plans, "failing", failingCommit.value)

        eventually { assertEquals(listOf(BuildSeen(BuildStatus.Failing, failingCommit), BuildSeen(BuildStatus.Passing, passingCommit)), bob.sees(plans).builds) }
    }

    @Test
    fun `a build report CI can't read is refused with every reason`() {
        assertEquals(
            Verdict.Rejected("A build is passing, failing or running", "A commit is 7 to 40 hex characters"),
            driver.ciReports(plans, "exploded", "nope"),
        )
    }
}