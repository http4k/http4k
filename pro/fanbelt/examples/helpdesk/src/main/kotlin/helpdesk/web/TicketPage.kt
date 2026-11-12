package helpdesk.web

import helpdesk.domain.Action
import helpdesk.domain.Viewer
import helpdesk.domain.actionsFor
import helpdesk.fakes.FakeEmailProvider
import helpdesk.feature.ActivityFeature
import helpdesk.feature.TicketsFeature
import helpdesk.view.ActivityView
import helpdesk.view.ConversationView
import helpdesk.view.FactView
import helpdesk.view.HeaderView
import helpdesk.view.MessageView
import helpdesk.view.ProviderView
import helpdesk.view.QueueRow
import helpdesk.view.QueueView
import helpdesk.view.ReplySignals
import helpdesk.view.TicketGoneView
import helpdesk.view.TicketPageView
import helpdesk.view.ViewingView
import helpdesk.view.describe
import helpdesk.view.nameOf
import org.http4k.fanbelt.bus.Subscription
import org.http4k.fanbelt.page.Layout
import org.http4k.fanbelt.page.Page
import org.http4k.fanbelt.page.Region
import org.http4k.fanbelt.page.Viewing
import org.http4k.fanbelt.state.join
import org.http4k.fanbelt.tenancy.Workspace
import org.http4k.template.TemplateRenderer

fun TicketPage(
    workspace: Workspace,
    tickets: TicketsFeature,
    activity: ActivityFeature,
    nav: Region<*, *>,
    provider: FakeEmailProvider,
    renderer: TemplateRenderer,
): Page {
    val queue = queue(tickets, renderer)
    val header = header(tickets, renderer)
    val conversation = conversation(tickets, renderer)
    val history = activity(activity, renderer)
    val viewing = viewing(tickets, renderer)

    return Page(
        workspace = workspace,
        layout = Layout(
            regions = listOf(nav, queue, header, conversation, history, viewing),
            renderer = renderer,
            subject = header,
            signals = ReplySignals(),
        ) { request, shell -> TicketPageView(shell, ticketId(request), principal(request).name, ProviderView(provider.isUp)) },
        viewing = Viewing { scope, request ->
            val me = principal(request).id
            val online = tickets.online.join(scope, me)
            val viewing = tickets.viewing.join(scope, ticketId(request), me)
            Subscription {
                online.close()
                viewing.close()
            }
        },
    )
}

private fun queue(tickets: TicketsFeature, renderer: TemplateRenderer) =
    Region("queue", renderer, tickets.queue) { tickets, request ->
        val selected = ticketId(request)
        QueueView(tickets.map { QueueRow(it.id, it.number, it.subject, it.status, nameOf(it.assignee), it.id == selected) })
    }

private fun header(tickets: TicketsFeature, renderer: TemplateRenderer) =
    Region("header", renderer, ticketId, tickets.ticket) { detail, request ->
        if (detail == null) return@Region TicketGoneView
        val ticket = detail.ticket
        val actions = ticket.actionsFor(principal(request).id)
        HeaderView(
            ticket.id, ticket.number, ticket.subject, ticket.customer, ticket.status, nameOf(ticket.assignee),
            canAssign = Action.Take in actions,
            canPend = Action.Pend in actions,
            canResolve = Action.Resolve in actions,
            canReopen = Action.Reopen in actions,
            notification = detail.notification,
        )
    }

private fun conversation(tickets: TicketsFeature, renderer: TemplateRenderer) =
    Region("conversation", renderer, ticketId, tickets.conversation) { conversation, _ ->
        if (conversation == null) return@Region ConversationView(emptyList())
        ConversationView(conversation.replies.map { MessageView(nameOf(it.agent) ?: conversation.customer.value, it.agent == null, it.body) })
    }

private fun activity(activity: ActivityFeature, renderer: TemplateRenderer) =
    Region("activity", renderer, ticketId, activity.activity) { facts, _ ->
        ActivityView(facts.map { fact -> FactView(nameOf(fact.actor) ?: fact.customer?.value ?: "Helpdesk", fact.describe()) })
    }

private fun viewing(tickets: TicketsFeature, renderer: TemplateRenderer) =
    Region("viewing", renderer, ticketId, tickets.viewing) { here, request ->
    val me = principal(request).id
    ViewingView(here.keys.map { if (it == me) "${Viewer(it).name} (you)" else Viewer(it).name }.sorted())
}