package helpdesk.feature

import helpdesk.domain.Action.Pend
import helpdesk.domain.Action.Reopen
import helpdesk.domain.Action.Resolve
import helpdesk.domain.Action.Take
import helpdesk.domain.Actor
import helpdesk.domain.AgentsModel
import helpdesk.domain.Conversation
import helpdesk.domain.CustomerCard
import helpdesk.domain.Failure
import helpdesk.domain.NavModel
import helpdesk.domain.NewTicket
import helpdesk.domain.OutboxStatus
import helpdesk.domain.Status
import helpdesk.domain.Status.Open
import helpdesk.domain.Status.Pending
import helpdesk.domain.Status.Resolved
import helpdesk.domain.StatusChange
import helpdesk.domain.TicketDetail
import helpdesk.domain.TicketId
import helpdesk.domain.ViewerId
import helpdesk.domain.actionsFor
import helpdesk.domain.replyBody
import helpdesk.store.HelpdeskTables
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.fanbelt.feature.ActorKey
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.flatMap
import org.http4k.fanbelt.feature.map
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.topic
import org.http4k.fanbelt.tenancy.Scope

class TicketsFeature(private val runtime: AppRuntime, private val policy: OrgPolicy) : Feature<Tickets, Actor>({ tables -> Tickets(HelpdeskTables(tables), runtime.random) }) {
    val queue by state { it.queue() }

    val ticket by state { desk, id: TicketId ->
        val found = desk.ticket(id)
        if (found == null) null else TicketDetail(found, desk.notification(id))
    }

    val conversation by state { desk, id: TicketId ->
        val found = desk.ticket(id)
        if (found == null) null else Conversation(found.customer, desk.replies(id))
    }

    val customer by state { desk, id: TicketId ->
        val found = desk.ticket(id)
        if (found == null) null else CustomerCard(found.customer, desk.ticketsFrom(found.customer))
    }

    val online by presence(ViewerId)

    val agents by derived { ctx, _: Unit ->
        ctx.watch(queue.topic())
        ctx.stale(at = policy.recheckAt())
        val acted = ctx.domain.agents()
        val here = ctx.read(online).keys
        val agents = policy.agentsAmong(ctx.tenant, acted + here)
        val known = acted intersect agents
        AgentsModel(known, here intersect agents, known.associateWith(ctx.domain::openFor))
    }

    val counts by derived { ctx, _: Unit ->
        ctx.watch(queue.topic())
        ctx.domain.counts()
    }

    val nav by derived { ctx, _: Unit -> NavModel(ctx.read(counts), ctx.read(agents).open, ctx.read(online).keys) }

    val viewing by presence(TicketId, ViewerId)

    val anAgent by rule { ctx, actor ->
        ctx.stale(at = policy.recheckAt())
        if (actor is Actor.Agent && policy.isAgent(ctx.tenant, actor.id)) Allowed(actor) else Denied
    }

    val aCustomer by rule { ctx, actor -> if (actor is Actor.Customer) Allowed(actor) else Denied }

    val system by rule { ctx, actor -> if (actor is Actor.System) Allowed(actor) else Denied }

    val agentOnTicket by rule { ctx, actor, id: TicketId ->
        ctx.read(anAgent, ActorKey(actor, Unit)).flatMap { agent -> if (ctx.domain.ticket(id) != null) Allowed(agent) else Denied }
    }

    val firstInQueue by rule { ctx, actor ->
        ctx.read(anAgent, ActorKey(actor, Unit)).map { ctx.domain.queue().firstOrNull()?.id }
    }

    val customerOf by rule { ctx, actor, id: TicketId ->
        ctx.read(anAgent, ActorKey(actor, Unit)).map { ctx.read(customer, id) }
    }

    val yourTicket by rule { ctx, actor, id: TicketId ->
        val found = ctx.domain.ticket(id)
        if (actor is Actor.Customer && found?.customer == actor.email) Allowed(found) else Denied
    }

    val takingAgent by rule { ctx, actor, id: TicketId ->
        ctx.read(anAgent, ActorKey(actor, Unit)).flatMap { agent -> if (Take in ctx.domain.actionsOf(id, agent)) Allowed(agent) else Denied }
    }

    val changingAgent by rule { ctx, actor, change: StatusChange ->
        when (actor) {
            is Actor.Agent if change.status.requires() in ctx.domain.actionsOf(change.ticket, actor) -> Allowed(actor)

            else -> Denied
        }
    }

    val open by operation(aCustomer) { desk, _, new: NewTicket, actor ->
        val id = desk.open(new, actor.email)
        Changes.of(recorded(id), id)
    }

    val reply by operation(agentOnTicket, Pair<TicketId, String>::first) { desk, actor, (id, raw), agent ->
        val body = replyBody(raw).orElse { return@operation it }
        desk.reply(id, agent.id, body)
        Changes.of(recorded(id) + conversation.topic(id))
    }

    val customerReply by operation(yourTicket, Pair<TicketId, String>::first) { desk, _, (id, raw), ticket ->
        val body = replyBody(raw).orElse { return@operation it }
        desk.customerReplies(id, ticket.customer, body)
        Changes.of(recorded(id) + conversation.topic(id))
    }

    val take by operation(takingAgent) { desk, _, id, agent ->
        desk.assign(id, agent.id)
        Changes.of(recorded(id))
    }

    val setStatus by operation(changingAgent) { desk, _, change, agent ->
        desk.setStatus(change, agent.id)
        Changes.of(recorded(change.ticket) + if (change.status == Resolved) setOf(notify.topic(change.ticket)) else emptySet())
    }

    val notified by operation(system) { desk, _, id: TicketId, _ ->
        desk.notified(id)
        Changes.of(recorded(id))
    }

    val notifyFailed by operation(system) { desk, _, failure: Failure, _ ->
        desk.notifyFailed(failure)
        Changes.of(recorded(failure.ticket))
    }

    val outbox by state { desk, id: TicketId -> desk.outbox(id) }

    val notify by job(outbox) { scope, outbox ->
        if (outbox?.status == OutboxStatus.Queued) send(scope, outbox.id)
    }

    private fun send(scope: Scope, id: TicketId) {
        val ticket = scope.read(this.ticket, id)?.ticket ?: return
        val sent = runtime.http(Request(POST, "/send").form("to", ticket.customer.value).form("subject", "Resolved: ${ticket.subject}"))
        if (!sent.status.successful) {
            scope.execute(notifyFailed, Actor.System, Failure(id, sent.status.toString()))
            error("Notification for $id not sent: ${sent.status}")
        }
        scope.execute(notified, Actor.System, id)
    }

    private fun recorded(id: TicketId) = setOf(ticket.topic(id), queue.topic(), outbox.topic(id))

    private fun Tickets.actionsOf(id: TicketId, agent: Actor.Agent) = ticket(id)?.actionsFor(agent.id).orEmpty()

    private fun Status.requires() = when (this) {
        Open -> Reopen
        Pending -> Pend
        Resolved -> Resolve
    }
}
