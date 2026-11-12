package helpdesk.view

import helpdesk.domain.CustomerEmail
import helpdesk.domain.Fact
import helpdesk.domain.FactKind.Assigned
import helpdesk.domain.FactKind.Notified
import helpdesk.domain.FactKind.NotifyFailed
import helpdesk.domain.FactKind.Opened
import helpdesk.domain.FactKind.Replied
import helpdesk.domain.FactKind.StatusChanged
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import helpdesk.domain.Viewer
import helpdesk.domain.ViewerId
import org.http4k.core.Uri
import org.http4k.fanbelt.page.PageShell
import org.http4k.fanbelt.store.Cursor
import org.http4k.template.ViewModel

data class OnlineAgent(val id: ViewerId, val name: String, val you: Boolean)

data class NavView(
    val onAgents: Boolean,
    val open: Int,
    val pending: Int,
    val resolved: Int,
    val mine: Int,
    val online: List<OnlineAgent>,
) : ViewModel

data class ProviderView(val up: Boolean) : ViewModel

fun Fact.describe() = when (kind) {
    Opened -> "opened the ticket"
    Assigned -> "took the ticket"
    StatusChanged -> "set the status to $detail"
    Replied -> "replied"
    Notified -> "customer notified by email"
    NotifyFailed -> "email to customer failed ($detail)"
}

fun nameOf(agent: ViewerId?) = agent?.let { Viewer(it).name }

data class CustomerTicket(val number: Int, val subject: Subject, val status: Status)

data class CustomerCardView(val customer: CustomerEmail, val tickets: List<CustomerTicket>) : ViewModel

data object CustomerCardGoneView : ViewModel

data class OrgLink(val name: String, val href: Uri)

data class OrgsPageView(val orgs: List<OrgLink>, val typed: String, val error: String?) : ViewModel

data class AgentRow(val id: ViewerId, val name: String, val online: Boolean, val open: Int, val selected: Boolean)

data class AgentsView(val rows: List<AgentRow>) : ViewModel

data class AgentTicket(val id: TicketId, val number: Int, val subject: Subject, val status: Status)

data class AgentFact(val ticket: TicketId, val number: Int, val text: String)

data class AgentView(
    val id: ViewerId,
    val name: String,
    val you: Boolean,
    val tickets: List<AgentTicket>,
    val facts: List<AgentFact>,
    val older: Cursor?,
    val paged: Boolean,
) : ViewModel

data class AgentsPageView(val shell: PageShell, val you: String, val provider: ProviderView) : ViewModel

data class ReplySignals(val reply: String = "")

data class QueueRow(
    val id: TicketId,
    val number: Int,
    val subject: Subject,
    val status: Status,
    val assignee: String?,
    val selected: Boolean
)

data class QueueView(val rows: List<QueueRow>) : ViewModel

data class HeaderView(
    val id: TicketId,
    val number: Int,
    val subject: Subject,
    val customer: CustomerEmail,
    val status: Status,
    val assignee: String?,
    val canAssign: Boolean,
    val canPend: Boolean,
    val canResolve: Boolean,
    val canReopen: Boolean,
    val notification: String?,
) : ViewModel

data class MessageView(val author: String, val customer: Boolean, val body: ReplyBody)

data class ConversationView(val messages: List<MessageView>) : ViewModel

data object TicketGoneView : ViewModel

data class FactView(val actor: String, val text: String)

data class ActivityView(val facts: List<FactView>) : ViewModel

data class ViewingView(val names: List<String>) : ViewModel

data class TicketPageView(val shell: PageShell, val ticket: TicketId, val you: String, val provider: ProviderView) : ViewModel