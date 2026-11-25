package helpdesk

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.CustomerEmail
import helpdesk.domain.ReplyBody
import helpdesk.domain.Subject
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test

private val di = CustomerEmail.of("di@example.com")
private val eve = CustomerEmail.of("eve@example.com")
private val invoiceIsWrong = Subject.of("Invoice is wrong")
private val exportTimesOut = Subject.of("Export times out")
private val clearYourCookies = ReplyBody.of("Try clearing your cookies")

interface AgentAccessContract {
    val driver: HelpdeskDriver

    @Test
    fun `an agent the org removes can neither open a ticket, take it nor reply, and drops out of the agents list, while others carry on`() {
        val sam = driver.agent("sam")
        val alex = driver.agent("alex")
        val ticket = driver.customerOpens(invoiceIsWrong, from = di)
        driver.orgRemoves("sam")

        sam.takes(ticket)
        assertThat(sam.replies(ticket, clearYourCookies.value), equalTo(Verdict.Denied))
        assertThat(alex.replies(ticket, clearYourCookies.value), equalTo(Verdict.Accepted))
        assertThat(sam.opens(ticket), equalTo(false))
        assertThat(alex.opens(ticket), equalTo(true))

        eventually {
            assertThat(alex.sees(ticket).assignee, absent())
            assertThat(alex.seesAgents(), equalTo(setOf(alex.name)))
        }
    }

    @Test
    fun `an agent the org removes while watching a ticket is sent away when the policy is next checked`() {
        val sam = driver.agent("sam")
        val ticket = driver.customerOpens(exportTimesOut, from = eve)
        sam.watches(ticket)

        driver.orgRemoves("sam")
        driver.policyRechecked()

        eventually { assertThat(sam.isOn(ticket), equalTo(false)) }
    }
}
