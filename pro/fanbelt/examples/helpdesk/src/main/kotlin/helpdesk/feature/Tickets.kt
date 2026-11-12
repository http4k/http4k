package helpdesk.feature

import dev.forkhandles.values.random
import helpdesk.domain.Counts
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Fact
import helpdesk.domain.FactId
import helpdesk.domain.FactKind
import helpdesk.domain.FactKind.Assigned
import helpdesk.domain.FactKind.Notified
import helpdesk.domain.FactKind.NotifyFailed
import helpdesk.domain.FactKind.Opened
import helpdesk.domain.FactKind.Replied
import helpdesk.domain.FactKind.StatusChanged
import helpdesk.domain.Failure
import helpdesk.domain.KnownAgent
import helpdesk.domain.NewTicket
import helpdesk.domain.Outbox
import helpdesk.domain.OutboxStatus
import helpdesk.domain.Reply
import helpdesk.domain.ReplyBody
import helpdesk.domain.ReplyId
import helpdesk.domain.Status
import helpdesk.domain.StatusChange
import helpdesk.domain.Subject
import helpdesk.domain.Ticket
import helpdesk.domain.TicketId
import helpdesk.domain.ViewerId
import helpdesk.store.HelpdeskTables
import org.http4k.fanbelt.store.Cursor
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.first
import org.http4k.fanbelt.store.neq
import java.time.Duration
import java.time.Instant
import java.util.Random

private val SENDING = Duration.ofSeconds(30)

private const val ATTEMPTS = 4

class Journal(private val tables: HelpdeskTables) {
    fun activity(id: TicketId) = tables.journal.page(where = Fact::ticket eq id, order = Newest).items

    fun activityBy(agent: ViewerId, before: Cursor?, limit: Int) = tables.journal.page(where = Fact::actor eq agent, order = Newest, after = before, limit = limit)
}

class Tickets(private val tables: HelpdeskTables, private val random: Random) {
    fun queue() = tables.tickets.page().sortedWith(queueOrder)

    fun ticket(id: TicketId) = tables.tickets.get(id)

    fun ticketsFrom(customer: CustomerEmail) = tables.tickets.page(where = Ticket::customer eq customer).sortedWith(queueOrder)

    fun replies(id: TicketId) = tables.replies.page(where = Reply::ticket eq id).items

    fun agents() = tables.agents.page().map { it.id }.toSet()

    fun counts() = Counts(
        open = tables.tickets.count(Ticket::status eq Status.Open),
        pending = tables.tickets.count(Ticket::status eq Status.Pending),
        resolved = tables.tickets.count(Ticket::status eq Status.Resolved),
    )

    fun openFor(agent: ViewerId) = tables.tickets.count((Ticket::assignee eq agent) and (Ticket::status neq Status.Resolved))

    fun open(ticket: NewTicket, customer: CustomerEmail): TicketId {
        val id = TicketId.random(random)
        tables.tickets.insert(Ticket(id, nextNumber(), ticket.subject, customer, Status.Open, null))
        tables.replies.insert(Reply(ReplyId.random(random), id, null, ticket.body))
        record(id, null, Opened, customer = customer)
        return id
    }

    fun reply(id: TicketId, agent: ViewerId, body: ReplyBody) {
        tables.replies.insert(Reply(ReplyId.random(random), id, agent, body))
        record(id, agent, Replied)
    }

    fun customerReplies(id: TicketId, customer: CustomerEmail, body: ReplyBody) {
        tables.replies.insert(Reply(ReplyId.random(random), id, null, body))
        record(id, null, Replied, customer = customer)
    }

    fun assign(id: TicketId, agent: ViewerId) {
        tables.tickets.update(id) { it.copy(assignee = agent) }
        record(id, agent, Assigned)
    }

    fun setStatus(change: StatusChange, agent: ViewerId) {
        tables.tickets.update(change.ticket) { it.copy(status = change.status) }
        record(change.ticket, agent, StatusChanged, change.status.name)
        if (change.status == Status.Resolved) {
            tables.outbox.delete(change.ticket)
            tables.outbox.insert(Outbox(change.ticket))
        }
    }

    fun notification(id: TicketId): String? {
        val pending = tables.outbox.get(id)
            ?: return if (tables.journal.first(where = (Fact::ticket eq id) and (Fact::kind eq Notified)) != null) "sent" else null
        return when {
            pending.status == OutboxStatus.GaveUp -> "gave up"
            pending.attempts == 0 -> "queued"
            else -> "retrying after ${pending.attempts} failed attempts"
        }
    }

    fun dueNotifications(now: Instant) =
        tables.outbox.page(where = Outbox::status eq OutboxStatus.Queued).filter { it.isDue(now) }.map { it.id }

    fun claimNotification(id: TicketId, now: Instant): Boolean {
        val due = tables.outbox.get(id)?.takeIf { it.status == OutboxStatus.Queued && it.isDue(now) } ?: return false
        tables.outbox.update(id) { due.copy(dueAt = now + SENDING) }
        return true
    }

    fun notified(id: TicketId) {
        tables.outbox.delete(id)
        record(id, null, Notified)
    }

    fun notifyFailed(failure: Failure) {
        record(failure.ticket, null, NotifyFailed, failure.reason)
        tables.outbox.update(failure.ticket) {
            val attempts = it.attempts + 1
            if (attempts >= ATTEMPTS) it.copy(status = OutboxStatus.GaveUp, attempts = attempts)
            else it.copy(attempts = attempts, dueAt = failure.at + Duration.ofSeconds(2L * attempts))
        }
    }

    private fun nextNumber() = (tables.tickets.first(order = Newest)?.number ?: 0) + 1

    private fun record(ticket: TicketId, actor: ViewerId?, kind: FactKind, detail: String = "", customer: CustomerEmail? = null) {
        tables.journal.insert(Fact(FactId.random(random), ticket, actor, kind, detail, customer))
        if (actor != null && tables.agents.get(actor) == null) tables.agents.insert(KnownAgent(actor))
    }
}

fun Tickets.seed() {
    if (queue().isNotEmpty()) return
    listOf(
        Triple(
            "Can't log in after password reset",
            "ana@example.com",
            "I reset my password and now the login page just reloads."
        ),
        Triple(
            "Invoice shows the wrong VAT number",
            "bo@example.com",
            "Our March invoice has last year's VAT number on it."
        ),
        Triple("Export to CSV times out", "cy@example.com", "Exporting more than a few thousand rows never finishes."),
        Triple("How do I add a teammate?", "di@example.com", "I can't find where to invite people to our workspace."),
        Triple(
            "Webhook deliveries are delayed",
            "ed@example.com",
            "Our endpoint is getting events 10 minutes late since Tuesday."
        ),
    ).forEach { (subject, customer, body) -> open(NewTicket(Subject.of(subject), ReplyBody.of(body)), CustomerEmail.of(customer)) }
}

private val queueOrder = compareBy<Ticket>({ it.status }, { it.number })