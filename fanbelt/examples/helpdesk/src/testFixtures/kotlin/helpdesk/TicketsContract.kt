package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.Action.Pend
import helpdesk.domain.Action.Reopen
import helpdesk.domain.Action.Resolve
import helpdesk.domain.Action.Take
import helpdesk.domain.CustomerEmail
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status.Open
import helpdesk.domain.Status.Resolved
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import java.util.UUID

private val ana = CustomerEmail.of("ana@example.com")
private val bo = CustomerEmail.of("bo@example.com")
private val cy = CustomerEmail.of("cy@example.com")
private val di = CustomerEmail.of("di@example.com")
private val cannotLogIn = Subject.of("Cannot log in")
private val billingQuestion = Subject.of("Billing question")
private val somethingElse = Subject.of("Something else")
private val exportTimesOut = Subject.of("Export times out")
private val clearYourCookies = ReplyBody.of("Try clearing your cookies")

interface TicketsContract {
    val driver: HelpdeskDriver

    @Test
    fun `resolving a ticket notifies the customer, and everyone watching sees it live`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(cannotLogIn, from = ana)

        alex.watches(ticket)
        sam.takes(ticket)
        sam.replies(ticket, clearYourCookies.value)
        sam.resolves(ticket)

        eventually {
            val seen = alex.sees(ticket)
            assertThat(seen.status, equalTo(Resolved))
            assertThat(seen.assignee, equalTo(sam.name))
            assertThat(seen.replies.last(), equalTo(clearYourCookies))
            assertThat(seen.actions, equalTo(setOf(Reopen)))
        }

        driver.jobsRun()

        eventually { assertThat(alex.sees(ticket).notification, equalTo("sent")) }
        eventually { assertThat(driver.emailsTo(ana), equalTo(listOf("Resolved: Cannot log in"))) }
    }

    @Test
    fun `a ticket that doesn't exist is shown as gone`() {
        val sam = driver.agent("sam")
        val real = driver.customerOpens(billingQuestion, from = bo)

        assertThat(sam.finds(real), equalTo(true))
        assertThat(sam.finds(TicketId.of(UUID(0, 1))), equalTo(false))
    }

    @Test
    fun `an agent can preview a ticket's customer, with all of that customer's tickets`() {
        val sam = driver.agent("sam")
        val billing = driver.customerOpens(billingQuestion, from = bo)
        driver.customerOpens(cannotLogIn, from = bo)
        driver.customerOpens(somethingElse, from = di)

        assertThat(sam.previewsCustomerOf(billing).sortedBy { it.value }, equalTo(listOf(billingQuestion, cannotLogIn)))
    }

    @Test
    fun `only the assignee can resolve a ticket`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(exportTimesOut, from = cy)
        alex.watches(ticket)

        sam.takes(ticket)

        eventually { assertThat(sam.sees(ticket).actions, equalTo(setOf(Pend, Resolve))) }
        eventually { assertThat(alex.sees(ticket).actions, equalTo(setOf(Take, Pend))) }

        assertThat(alex.resolves(ticket), equalTo(Verdict.Denied))
        assertThat(alex.sees(ticket).status, equalTo(Open))
    }
}
