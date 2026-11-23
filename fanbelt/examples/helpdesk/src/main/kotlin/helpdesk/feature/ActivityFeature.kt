package helpdesk.feature

import helpdesk.domain.Actor
import helpdesk.domain.AgentModel
import helpdesk.domain.AgentPage
import helpdesk.domain.Status
import helpdesk.domain.TicketId
import helpdesk.store.HelpdeskTables
import org.http4k.fanbelt.feature.Feature

class ActivityFeature(tickets: TicketsFeature) : Feature<Journal, Actor>({ tables -> Journal(HelpdeskTables(tables)) }) {
    val activity by derived { ctx, id: TicketId ->
        ctx.watch(tickets.ticket.topic(id))
        ctx.domain.activity(id)
    }

    val agentPage by derived { ctx, page: AgentPage ->
        val queue = ctx.read(tickets.queue)
        AgentModel(
            queue.filter { it.assignee == page.agent && it.status != Status.Resolved },
            ctx.domain.activityBy(page.agent, page.before, 10),
            queue.associate { it.id to it.number },
        )
    }
}