package helpdesk.feature

import helpdesk.domain.Action.*
import helpdesk.domain.Actor
import helpdesk.domain.AgentsModel
import helpdesk.domain.NavModel
import helpdesk.domain.Conversation
import helpdesk.domain.CustomerCard
import helpdesk.domain.Failure
import helpdesk.domain.NewTicket
import helpdesk.domain.Status
import helpdesk.domain.Status.*
import helpdesk.domain.StatusChange
import helpdesk.domain.TicketDetail
import helpdesk.domain.TicketId
import helpdesk.domain.ViewerId
import helpdesk.domain.actionsFor
import helpdesk.domain.replyBody
import helpdesk.store.HelpdeskTables
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.state.topic
import java.time.Instant
import java.util.Random

class TicketsFeature(random: Random) : Feature<Tickets, Actor>({ tables -> Tickets(HelpdeskTables(tables), random) }) {
    val queue by state { it.queue() }

    val ticket by state(TicketId) { desk, id ->
        val found = desk.ticket(id)
        if (found == null) null else TicketDetail(found, desk.notification(id))
    }

    val conversation by state(TicketId) { desk, id ->
        val found = desk.ticket(id)
        if (found == null) null else Conversation(found.customer, desk.replies(id))
    }

    val customer by state(TicketId) { desk, id ->
        val found = desk.ticket(id)
        if (found == null) null else CustomerCard(found.customer, desk.ticketsFrom(found.customer))
    }

    val online by presence(ViewerId)

    val agents by derived { ctx, _: Unit ->
        ctx.watch(queue.topic())
        val known = ctx.domain.agents()
        AgentsModel(known, ctx.read(online).keys, known.associateWith(ctx.domain::openFor))
    }

    val counts by derived { ctx, _: Unit ->
        ctx.watch(queue.topic())
        ctx.domain.counts()
    }

    val nav by derived { ctx, _: Unit -> NavModel(ctx.read(counts), ctx.read(agents).open, ctx.read(online).keys) }

    val viewing by presence(TicketId, ViewerId)

    val aCustomer by rule { _, actor -> if (actor is Actor.Customer) Allowed(actor) else Denied }

    val system by rule { _, actor -> if (actor is Actor.System) Allowed(actor) else Denied }

    val agentOnTicket by rule { ctx, actor, id: TicketId ->
        if (actor is Actor.Agent && ctx.domain.ticket(id) != null) Allowed(actor) else Denied
    }

    val yourTicket by rule { ctx, actor, id: TicketId ->
        val found = ctx.domain.ticket(id)
        if (actor is Actor.Customer && found?.customer == actor.email) Allowed(found) else Denied
    }

    val takingAgent by rule { ctx, actor, id: TicketId ->
        if (actor is Actor.Agent && Take in ctx.domain.actionsOf(id, actor)) Allowed(actor) else Denied
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

    val reply by operation(agentOnTicket, Pair<TicketId, String>::first) { desk, _, (id, raw), agent ->
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
        Changes.of(recorded(change.ticket))
    }

    val claimNotification by operation(system) { desk, _, (id, now): Pair<TicketId, Instant>, _ ->
        Changes.of(setOf(ticket.topic(id)), desk.claimNotification(id, now))
    }

    val notified by operation(system) { desk, _, id: TicketId, _ ->
        desk.notified(id)
        Changes.of(recorded(id))
    }

    val notifyFailed by operation(system) { desk, _, failure: Failure, _ ->
        desk.notifyFailed(failure)
        Changes.of(recorded(failure.ticket))
    }

    private fun recorded(id: TicketId) = setOf(ticket.topic(id), queue.topic())

    private fun Tickets.actionsOf(id: TicketId, agent: Actor.Agent) = ticket(id)?.actionsFor(agent.id).orEmpty()

    private fun Status.requires() = when (this) {
        Open -> Reopen
        Pending -> Pend
        Resolved -> Resolve
    }
}