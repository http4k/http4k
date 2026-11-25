package board

import board.domain.BoardId
import board.domain.BuildStatus
import board.domain.Commit
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val plans = BoardId.of("plans")
private val passingCommit = Commit.of("abc1234")
private val failingCommit = Commit.of("def5678")

interface BuildsContract {
    val driver: BoardDriver

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
