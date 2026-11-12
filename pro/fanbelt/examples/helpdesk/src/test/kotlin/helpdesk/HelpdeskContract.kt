package helpdesk

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
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class HelpdeskContract {
    abstract val driver: HelpdeskDriver

    private val ana = CustomerEmail.of("ana@example.com")
    private val bo = CustomerEmail.of("bo@example.com")
    private val cy = CustomerEmail.of("cy@example.com")
    private val di = CustomerEmail.of("di@example.com")
    private val eve = CustomerEmail.of("eve@example.com")

    private val cannotLogIn = Subject.of("Cannot log in")
    private val billingQuestion = Subject.of("Billing question")
    private val somethingElse = Subject.of("Something else")
    private val invoiceIsWrong = Subject.of("Invoice is wrong")
    private val exportTimesOut = Subject.of("Export times out")
    private val addingATeammate = Subject.of("How do I add a teammate?")

    private val pleaseHelp = ReplyBody.of("Please help")
    private val clearYourCookies = ReplyBody.of("Try clearing your cookies")
    private val anyUpdate = ReplyBody.of("Any update?")
    private val hijack = ReplyBody.of("Hijack")

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
            assertEquals(Resolved, seen.status)
            assertEquals(sam.name, seen.assignee)
            assertEquals(clearYourCookies, seen.replies.last())
            assertEquals(setOf(Reopen), seen.actions)
        }

        driver.jobsRun()

        eventually { assertEquals("sent", alex.sees(ticket).notification) }
        eventually { assertEquals(listOf("Resolved: Cannot log in"), driver.emailsTo(ana)) }
    }

    @Test
    fun `a customer's reply appears live for agents watching the ticket`() {
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(billingQuestion, from = bo)
        alex.watches(ticket)

        assertEquals(Verdict.Accepted, driver.customerReplies(ticket, from = bo, text = anyUpdate))

        eventually { assertEquals(listOf(pleaseHelp, anyUpdate), alex.sees(ticket).replies) }
    }

    @Test
    fun `a ticket that doesn't exist is shown as gone`() {
        val sam = driver.agent("sam")
        val real = driver.customerOpens(billingQuestion, from = bo)

        assertEquals(true, sam.finds(real))
        assertEquals(false, sam.finds(TicketId.of(UUID(0, 1))))
    }

    @Test
    fun `an agent can preview a ticket's customer, with all of that customer's tickets`() {
        val sam = driver.agent("sam")
        val billing = driver.customerOpens(billingQuestion, from = bo)
        driver.customerOpens(cannotLogIn, from = bo)
        driver.customerOpens(somethingElse, from = di)

        assertEquals(listOf(billingQuestion, cannotLogIn), sam.previewsCustomerOf(billing).sortedBy { it.value })
    }

    @Test
    fun `customers can only reply to their own tickets`() {
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(billingQuestion, from = bo)
        alex.watches(ticket)

        assertEquals(Verdict.Denied, driver.customerReplies(ticket, from = eve, text = hijack))

        assertEquals(listOf(pleaseHelp), alex.sees(ticket).replies)
    }

    @Test
    fun `an empty reply is rejected for the sender only, and nothing changes for anyone else`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(invoiceIsWrong, from = bo)
        sam.watches(ticket)
        alex.watches(ticket)
        val before = alex.sees(ticket)

        val outcome = sam.replies(ticket, "   ")

        assertEquals(Verdict.Rejected("A reply can't be blank or longer than 2000 characters"), outcome)
        assertEquals(before, alex.sees(ticket))
    }

    @Test
    fun `only the assignee can resolve a ticket`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(exportTimesOut, from = cy)
        alex.watches(ticket)

        sam.takes(ticket)

        eventually { assertEquals(setOf(Pend, Resolve), sam.sees(ticket).actions) }
        eventually { assertEquals(setOf(Take, Pend), alex.sees(ticket).actions) }

        assertEquals(Verdict.Denied, alex.resolves(ticket))
        assertEquals(Open, alex.sees(ticket).status)
    }

    @Test
    fun `agents watching a ticket see who else is viewing it, live`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(addingATeammate, from = di)

        sam.watches(ticket)
        alex.watches(ticket)

        eventually { assertEquals(setOf(sam.name, alex.name), sam.sees(ticket).viewers) }

        alex.leaves(ticket)

        eventually(untilAClosedTabIsNoticed) { assertEquals(setOf(sam.name), sam.sees(ticket).viewers) }
    }
}