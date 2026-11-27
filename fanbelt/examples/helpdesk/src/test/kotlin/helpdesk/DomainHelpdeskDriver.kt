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
import helpdesk.fakes.Fakes
import helpdesk.feature.TicketsFeature
import helpdesk.view.nameOf
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import java.time.Duration

class DomainHelpdeskDriver : HelpdeskDriver {
    private val provider = FakeEmailProvider()
    private val tenant = TestTenant(TestRuntime(http = Fakes(provider)))
    private val policy = orgPolicy(tenant.runtime.http, tenant.clock)
    private val tickets = TicketsFeature(tenant.runtime, policy)

    init {
        tenant.runs(tickets.notify)
    }

    override fun agent(name: String): Agent = DomainAgent(viewerNamed(name).also { policy.appoint(tenant.id, it) })

    override fun orgRemoves(agent: String) = policy.dismiss(tenant.id, viewerNamed(agent))

    override fun policyRechecked() = tenant.tick(policy.recheckEvery)

    override fun customerOpens(subject: Subject, from: CustomerEmail) =
        tenant { tickets.open.given(Actor.Customer(from), NewTicket(subject, ReplyBody.of("Please help"))) }

    override fun customerReplies(ticket: TicketId, from: CustomerEmail, text: ReplyBody) =
        tenant { tickets.customerReply.execute(Actor.Customer(from), ticket to text.value) }

    override fun emailsTo(customer: CustomerEmail) = provider.sent.filter { it.to == customer.value }.map { it.subject }

    override fun jobsRun() = tenant.tick(Duration.ofSeconds(1))

    private inner class DomainAgent(private val id: ViewerId) : Agent {
        override val name = Viewer(id).name
        private val actor = Actor.Agent(id)
        private val visits = mutableMapOf<TicketId, Subscription>()

        override fun watches(ticket: TicketId) {
            visits[ticket] = tenant { tickets.viewing.join(ticket, id) }
            seen(ticket)
            opens(ticket)
        }

        override fun leaves(ticket: TicketId) {
            visits.remove(ticket)?.close()
        }

        override fun takes(ticket: TicketId) {
            tenant { tickets.take.execute(actor, ticket) }
        }

        override fun replies(ticket: TicketId, text: String) = tenant { tickets.reply.execute(actor, ticket to text) }

        override fun resolves(ticket: TicketId) = tenant { tickets.setStatus.execute(actor, StatusChange(ticket, Resolved)) }

        override fun sees(ticket: TicketId) = seen(ticket)

        override fun previewsCustomerOf(ticket: TicketId) =
            tenant { tickets.customer.read(ticket)?.tickets.orEmpty().map { it.subject } }

        override fun finds(ticket: TicketId) = tenant { tickets.ticket.read(ticket) != null }

        override fun isOn(ticket: TicketId) = ticket in visits && opens(ticket)

        override fun opens(ticket: TicketId) = tenant { tickets.anAgent.read(ActorKey<Actor, Unit>(actor, Unit)) is Allowed }

        override fun seesAgents() = tenant { tickets.agents.read().let { it.known + it.online }.map { Viewer(it).name }.toSet() }

        private fun seen(ticket: TicketId): TicketSeen {
            val detail = tenant { tickets.ticket.read(ticket) } ?: error("$ticket doesn't exist")
            val conversation = tenant { tickets.conversation.read(ticket) } ?: error("$ticket doesn't exist")
            return TicketSeen(
                detail.ticket.status,
                nameOf(detail.ticket.assignee),
                conversation.replies.map { it.body },
                detail.notification,
                detail.ticket.actionsFor(id),
                tenant { tickets.viewing.read(ticket).keys.map { Viewer(it).name }.toSet() },
            )
        }
    }
}