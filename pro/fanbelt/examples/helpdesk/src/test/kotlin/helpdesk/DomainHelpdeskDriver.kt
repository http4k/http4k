package helpdesk

import helpdesk.domain.Actor
import helpdesk.domain.CustomerEmail
import helpdesk.domain.NewTicket
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status.Resolved
import helpdesk.domain.StatusChange
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import helpdesk.domain.Viewer
import helpdesk.domain.ViewerId
import helpdesk.domain.actionsFor
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.TicketsFeature
import helpdesk.jobs.HelpdeskJobs
import helpdesk.view.nameOf
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.accepted
import java.time.Duration

class DomainHelpdeskDriver : HelpdeskDriver {
    private val provider = FakeEmailProvider()
    private val tenant = TestTenant(TestRuntime(http = provider))
    private val tickets = TicketsFeature(tenant.random)

    init {
        HelpdeskJobs(tenant.runtime, tenant.infra, tickets).start()
        tenant.settle()
    }

    override fun agent(name: String): Agent = DomainAgent(viewerNamed(name))

    override fun customerOpens(subject: Subject, from: CustomerEmail): TicketId {
        val opened = tenant.scope.execute(tickets.open, Actor.Customer(from), NewTicket(subject, ReplyBody.of("Please help")))
        tenant.settle()
        return opened.accepted()
    }

    override fun customerReplies(ticket: TicketId, from: CustomerEmail, text: ReplyBody) =
        tenant.execute(tickets.customerReply, Actor.Customer(from), ticket to text.value)

    override fun emailsTo(customer: CustomerEmail) = provider.sent.filter { it.to == customer.value }.map { it.subject }

    override fun jobsRun() = tenant.tick(Duration.ofSeconds(1))

    private inner class DomainAgent(private val id: ViewerId) : Agent {
        override val name = Viewer(id).name
        private val actor = Actor.Agent(id)
        private val visits = mutableMapOf<TicketId, Subscription>()

        override fun watches(ticket: TicketId) {
            visits[ticket] = tenant.join(tickets.viewing, ticket, id)
            seen(ticket)
        }

        override fun leaves(ticket: TicketId) {
            visits.remove(ticket)?.close()
        }

        override fun takes(ticket: TicketId) {
            tenant.execute(tickets.take, actor, ticket)
        }

        override fun replies(ticket: TicketId, text: String) = tenant.execute(tickets.reply, actor, ticket to text)

        override fun resolves(ticket: TicketId) = tenant.execute(tickets.setStatus, actor, StatusChange(ticket, Resolved))

        override fun sees(ticket: TicketId) = seen(ticket)

        override fun previewsCustomerOf(ticket: TicketId) =
            tenant.read(tickets.customer, ticket)?.tickets.orEmpty().map { it.subject }

        override fun finds(ticket: TicketId) = tenant.read(tickets.ticket, ticket) != null

        private fun seen(ticket: TicketId): TicketSeen {
            val detail = tenant.read(tickets.ticket, ticket) ?: error("$ticket doesn't exist")
            val conversation = tenant.read(tickets.conversation, ticket) ?: error("$ticket doesn't exist")
            return TicketSeen(
                detail.ticket.status,
                nameOf(detail.ticket.assignee),
                conversation.replies.map { it.body },
                detail.notification,
                detail.ticket.actionsFor(id),
                tenant.read(tickets.viewing, ticket).keys.map { Viewer(it).name }.toSet(),
            )
        }
    }
}