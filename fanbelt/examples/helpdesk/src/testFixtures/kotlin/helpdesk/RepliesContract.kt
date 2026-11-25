package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.CustomerEmail
import helpdesk.domain.ReplyBody
import helpdesk.domain.Subject
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val bo = CustomerEmail.of("bo@example.com")
private val eve = CustomerEmail.of("eve@example.com")
private val billingQuestion = Subject.of("Billing question")
private val invoiceIsWrong = Subject.of("Invoice is wrong")
private val pleaseHelp = ReplyBody.of("Please help")
private val anyUpdate = ReplyBody.of("Any update?")
private val hijack = ReplyBody.of("Hijack")

interface RepliesContract {
    val driver: HelpdeskDriver

    @Test
    fun `a customer's reply appears live for agents watching the ticket`() {
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(billingQuestion, from = bo)
        alex.watches(ticket)

        assertThat(driver.customerReplies(ticket, from = bo, text = anyUpdate), equalTo(Verdict.Accepted))

        eventually { assertThat(alex.sees(ticket).replies, equalTo(listOf(pleaseHelp, anyUpdate))) }
    }

    @Test
    fun `customers can only reply to their own tickets`() {
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(billingQuestion, from = bo)
        alex.watches(ticket)

        assertThat(driver.customerReplies(ticket, from = eve, text = hijack), equalTo(Verdict.Denied))

        assertThat(alex.sees(ticket).replies, equalTo(listOf(pleaseHelp)))
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

        assertThat(outcome, equalTo(Verdict.Rejected("A reply can't be blank or longer than 2000 characters")))
        assertThat(alex.sees(ticket), equalTo(before))
    }
}
