package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.Actor
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Failure
import helpdesk.domain.NewTicket
import helpdesk.domain.OutboxStatus
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status.Resolved
import helpdesk.domain.StatusChange
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.TicketsFeature
import org.http4k.connect.openfga.FakeOpenFga
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.accepted
import org.junit.jupiter.api.Test
import java.time.Duration

class NotificationsTest {
    private val sam = Actor.Agent(viewerNamed("sam"))
    private val ana = Actor.Customer(CustomerEmail.of("ana@example.com"))

    private inner class Harness(sending: Boolean = false) {
        val provider = FakeEmailProvider()
        val runtime = TestRuntime(http = provider)
        val policy = orgPolicy(FakeOpenFga())
        val tickets = TicketsFeature(runtime, policy)
        val tenant = TestTenant(runtime, jobs = if (sending) listOf(tickets) else emptyList())

        init {
            policy.appointEveryone(tenant.scope.tenant)
        }

        fun sent() = provider.sent.map { it.subject }

        fun queued(ticket: TicketId) = tenant.read(tickets.outbox, ticket)?.status == OutboxStatus.Queued

        fun resolved(): TicketId {
            val id = tenant.scope.execute(tickets.open, ana, NewTicket(Subject.of("Cannot log in"), ReplyBody.of("Please help"))).accepted()
            tenant.scope.execute(tickets.take, sam, id)
            tenant.scope.execute(tickets.setStatus, sam, StatusChange(id, Resolved))
            tenant.settle()
            return id
        }
    }

    @Test
    fun `resolving a ticket sends its notification straight away`() {
        val app = Harness(sending = true)

        app.resolved()

        assertThat(app.sent(), equalTo(listOf("Resolved: Cannot log in")))
    }

    @Test
    fun `a send that fails is tried again a second later`() {
        val app = Harness(sending = true)
        app.provider.set(available = false)
        app.resolved()
        app.provider.set(available = true)

        app.tenant.tick(Duration.ofMillis(999))
        val failedOnce = app.sent()
        app.tenant.tick(Duration.ofMillis(1))

        assertThat(failedOnce to app.sent(), equalTo(emptyList<String>() to listOf("Resolved: Cannot log in")))
    }

    @Test
    fun `a notification that keeps failing is given up on, and never sent`() {
        val app = Harness(sending = true)
        app.provider.set(available = false)
        val ticket = app.resolved()
        app.tenant.tick(Duration.ofMinutes(1))

        app.provider.set(available = true)
        app.tenant.tick(Duration.ofMinutes(1))

        assertThat(app.sent() to app.queued(ticket), equalTo(emptyList<String>() to false))
    }

    @Test
    fun `a sent notification is no longer queued`() {
        val app = Harness()
        val ticket = app.resolved()

        app.tenant.execute(app.tickets.notified, Actor.System, ticket)

        assertThat(app.queued(ticket), equalTo(false))
    }

    @Test
    fun `a failed notification stays queued until its attempts run out`() {
        val app = Harness()
        val ticket = app.resolved()

        app.tenant.execute(app.tickets.notifyFailed, Actor.System, Failure(ticket, "503"))
        val afterOne = app.queued(ticket)
        repeat(3) { app.tenant.execute(app.tickets.notifyFailed, Actor.System, Failure(ticket, "503")) }

        assertThat(afterOne to app.queued(ticket), equalTo(true to false))
    }
}
