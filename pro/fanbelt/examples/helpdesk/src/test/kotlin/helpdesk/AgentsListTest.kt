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
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.accepted
import org.junit.jupiter.api.Test

class AgentsListTest {
    @Test
    fun `with every user appointed, as in the demo, whoever has worked a ticket is on the agents list`() {
        val tenant = TestTenant(TestRuntime(http = Fakes(FakeEmailProvider())))
        val policy = orgPolicy(tenant.runtime.http).apply { appointEveryone(tenant.scope.tenant) }
        val tickets = TicketsFeature(tenant.runtime, policy)
        val sam = viewerNamed("sam")
        val ticket = tenant.scope.execute(tickets.open, Actor.Customer(CustomerEmail.of("ana@example.com")), NewTicket(Subject.of("Help"), ReplyBody.of("Please"))).accepted()

        tenant.execute(tickets.take, Actor.Agent(sam), ticket)

        assertThat(tenant.read(tickets.agents).known, equalTo(setOf(sam)))
    }
}
