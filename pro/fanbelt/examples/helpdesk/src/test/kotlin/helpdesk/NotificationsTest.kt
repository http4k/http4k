package helpdesk

import helpdesk.domain.Actor
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Failure
import helpdesk.domain.NewTicket
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status.Resolved
import helpdesk.domain.StatusChange
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.TicketsFeature
import helpdesk.jobs.HelpdeskJobs
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.accepted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class NotificationsTest {
    private val sam = Actor.Agent(viewerNamed("sam"))
    private val ana = Actor.Customer(CustomerEmail.of("ana@example.com"))

    private inner class Harness(sending: Boolean = false) {
        val provider = FakeEmailProvider()
        var duringSend: () -> Unit = {}
        val tenant = TestTenant(TestRuntime(http = { request ->
            duringSend()
            provider(request)
        }))
        val tickets = TicketsFeature(tenant.random)

        init {
            if (sending) HelpdeskJobs(tenant.runtime, tenant.infra, tickets).start()
        }

        fun sent() = provider.sent.map { it.subject }

        fun due() = tenant.scope.read(tickets) { it.dueNotifications(tenant.clock.instant()) }

        fun resolved(): TicketId {
            val id = tenant.scope.execute(tickets.open, ana, NewTicket(Subject.of("Cannot log in"), ReplyBody.of("Please help"))).accepted()
            tenant.execute(tickets.take, sam, id)
            tenant.execute(tickets.setStatus, sam, StatusChange(id, Resolved))
            return id
        }
    }

    @Test
    fun `while one notification is being sent, the rest stay due for another node to take`() {
        val app = Harness(sending = true)
        app.resolved()
        val second = app.resolved()
        val stillDue = mutableListOf<List<TicketId>>()
        app.duringSend = { stillDue += app.due() }

        app.tenant.tick(Duration.ofSeconds(1))

        assertEquals(listOf(second), stillDue.first())
    }

    @Test
    fun `resolving a ticket sends its notification at the next sweep`() {
        val app = Harness(sending = true)

        app.resolved()
        app.tenant.tick(Duration.ofSeconds(1))

        assertEquals(listOf("Resolved: Cannot log in"), app.sent())
    }

    @Test
    fun `a send that fails is retried once its backoff ends`() {
        val app = Harness(sending = true)
        app.provider.set(available = false)
        app.resolved()
        app.tenant.tick(Duration.ofSeconds(1))

        app.provider.set(available = true)
        app.tenant.tick(Duration.ofSeconds(3))

        assertEquals(listOf("Resolved: Cannot log in"), app.sent())
    }

    @Test
    fun `a claimed notification isn't due`() {
        val app = Harness()
        val ticket = app.resolved()

        app.tenant.execute(app.tickets.claimNotification, Actor.System, ticket to app.tenant.clock.instant())

        assertEquals(emptyList<TicketId>(), app.due())
    }

    @Test
    fun `a claim that's never settled falls due again when it expires`() {
        val app = Harness()
        val ticket = app.resolved()
        app.tenant.execute(app.tickets.claimNotification, Actor.System, ticket to app.tenant.clock.instant())

        app.tenant.tick(Duration.ofSeconds(30))

        assertEquals(listOf(ticket), app.due())
    }

    @Test
    fun `a sent notification isn't due`() {
        val app = Harness()
        val ticket = app.resolved()

        app.tenant.execute(app.tickets.notified, Actor.System, ticket)

        assertEquals(emptyList<TicketId>(), app.due())
    }

    @Test
    fun `a failed notification backs off, then falls due again`() {
        val app = Harness()
        val ticket = app.resolved()

        app.tenant.execute(app.tickets.notifyFailed, Actor.System, Failure(ticket, "503", app.tenant.clock.instant()))
        val backingOff = app.due()
        app.tenant.tick(Duration.ofSeconds(2))

        assertEquals(emptyList<TicketId>(), backingOff)
        assertEquals(listOf(ticket), app.due())
    }

    @Test
    fun `a notification that keeps failing is given up on, and never falls due again`() {
        val app = Harness()
        val ticket = app.resolved()

        repeat(4) { app.tenant.execute(app.tickets.notifyFailed, Actor.System, Failure(ticket, "503", app.tenant.clock.instant())) }
        app.tenant.tick(Duration.ofMinutes(1))

        assertEquals(emptyList<TicketId>(), app.due())
    }
}
