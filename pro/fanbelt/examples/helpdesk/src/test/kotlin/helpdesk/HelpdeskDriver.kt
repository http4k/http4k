package helpdesk

import helpdesk.domain.Action
import helpdesk.domain.CustomerEmail
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import org.http4k.fanbelt.testing.Verdict

interface HelpdeskDriver {
    fun agent(name: String): Agent
    fun customerOpens(subject: Subject, from: CustomerEmail): TicketId
    fun customerReplies(ticket: TicketId, from: CustomerEmail, text: ReplyBody): Verdict
    fun emailsTo(customer: CustomerEmail): List<String>
    fun jobsRun()
}

interface Agent {
    val name: String
    fun watches(ticket: TicketId)
    fun leaves(ticket: TicketId)
    fun takes(ticket: TicketId)
    fun replies(ticket: TicketId, text: String): Verdict
    fun resolves(ticket: TicketId): Verdict
    fun sees(ticket: TicketId): TicketSeen
    fun finds(ticket: TicketId): Boolean
    fun previewsCustomerOf(ticket: TicketId): List<Subject>
}

data class TicketSeen(
    val status: Status,
    val assignee: String?,
    val replies: List<ReplyBody>,
    val notification: String?,
    val actions: Set<Action>,
    val viewers: Set<String>,
)