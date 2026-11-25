package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.Actor
import helpdesk.domain.CustomerEmail
import helpdesk.domain.NewTicket
import helpdesk.domain.ReplyBody
import helpdesk.domain.Subject
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.TicketsFeature
import org.http4k.connect.openfga.FakeOpenFga
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.accepted
import org.http4k.fanbelt.testing.asVerdict
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.junit.jupiter.api.Test

class NewOrgTest {
    @Test
    fun `everyone can work as an agent in an organisation created from the home page`() {
        val runtime = TestRuntime()
        val policy = orgPolicy(FakeOpenFga())
        lateinit var infra: AppInfra
        val http = App(runtime, { AppInfra(it).also { built -> infra = built } }, FakeEmailProvider(), policy).apply { start() }.handler.http!!
        val tickets = TicketsFeature(runtime, policy)

        http(Request(POST, "/orgs").form("org", "globex"))

        val globex = infra.scope(TenantId.of("globex"))
        val ticket = globex.execute(tickets.open, Actor.Customer(CustomerEmail.of("ana@example.com")), NewTicket(Subject.of("Help"), ReplyBody.of("Please"))).accepted()
        assertThat(globex.execute(tickets.take, Actor.Agent(viewerNamed("sam")), ticket).asVerdict(), equalTo(Verdict.Accepted))
    }
}
