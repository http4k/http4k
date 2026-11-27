package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.Actor
import helpdesk.domain.CustomerEmail
import helpdesk.domain.NewTicket
import helpdesk.domain.ReplyBody
import helpdesk.domain.Subject
import helpdesk.fakes.FakeEmailProvider
import helpdesk.fakes.Fakes
import helpdesk.feature.TicketsFeature
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test

class AgentsListTest {
    @Test
    fun `with every user appointed, as in the demo, whoever has worked a ticket is on the agents list`() {
        val tenant = TestTenant(TestRuntime(http = Fakes(FakeEmailProvider())))
        val policy = orgPolicy(tenant.runtime.http).apply { appointEveryone(tenant.id) }
        val tickets = TicketsFeature(tenant.runtime, policy)
        val sam = viewerNamed("sam")
        tenant {
            val ticket = tickets.open.given(Actor.Customer(CustomerEmail.of("ana@example.com")), NewTicket(Subject.of("Help"), ReplyBody.of("Please")))

            tickets.take.execute(Actor.Agent(sam), ticket)

            assertThat(tickets.agents.read().known, equalTo(setOf(sam)))
        }
    }
}
