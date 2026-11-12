package helpdesk.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.length
import dev.forkhandles.values.regex
import helpdesk.domain.Action.Pend
import helpdesk.domain.Action.Reopen
import helpdesk.domain.Action.Resolve
import helpdesk.domain.Action.Take
import helpdesk.domain.Status.Open
import helpdesk.domain.Status.Resolved
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Cursor
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Rows
import java.time.Instant
import java.util.UUID

class TicketId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<TicketId>(::TicketId)
}

class ReplyId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<ReplyId>(::ReplyId)
}

class FactId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<FactId>(::FactId)
}

class Subject private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<Subject, String>(::Subject, (1..120).length.and { it.isNotBlank() }, String::trim)
}

class CustomerEmail private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<CustomerEmail>(::CustomerEmail, "[^@\\s]+@[^@\\s]+".regex)
}

class ReplyBody private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<ReplyBody, String>(::ReplyBody, (1..2000).length.and { it.isNotBlank() }, String::trim)
}

enum class Status { Open, Pending, Resolved }

enum class Action { Take, Pend, Resolve, Reopen }

enum class FactKind { Opened, Assigned, StatusChanged, Replied, Notified, NotifyFailed }

data class Ticket(
    override val id: TicketId,
    val number: Int,
    val subject: Subject,
    val customer: CustomerEmail,
    val status: Status,
    val assignee: ViewerId?,
) : Identified<TicketId>

data class Reply(override val id: ReplyId, val ticket: TicketId, val agent: ViewerId?, val body: ReplyBody) : Identified<ReplyId>

data class Fact(override val id: FactId, val ticket: TicketId, val actor: ViewerId?, val kind: FactKind, val detail: String, val customer: CustomerEmail? = null) : Identified<FactId>

fun Ticket.actionsFor(agent: ViewerId): Set<Action> = setOfNotNull(
    Take.takeIf { assignee != agent && status != Resolved },
    Pend.takeIf { status == Open },
    Resolve.takeIf { assignee == agent && status != Resolved },
    Reopen.takeIf { status == Resolved },
)

fun replyBody(raw: String) = FieldName.of("reply").check(ReplyBody, raw, "A reply can't be blank or longer than 2000 characters")

data class NewTicket(val subject: Subject, val body: ReplyBody)

data class StatusChange(val ticket: TicketId, val status: Status)

data class Counts(val open: Int, val pending: Int, val resolved: Int)

data class Failure(val ticket: TicketId, val reason: String, val at: Instant)

data class KnownAgent(override val id: ViewerId) : Identified<ViewerId>

enum class OutboxStatus { Queued, GaveUp }

data class Outbox(override val id: TicketId, val status: OutboxStatus = OutboxStatus.Queued, val attempts: Int = 0, val dueAt: Instant? = null) : Identified<TicketId> {
    fun isDue(now: Instant) = dueAt == null || dueAt <= now
}

data class Org(val createdAt: Instant, override val id: String = SELF) : Identified<String> {
    companion object {
        const val SELF = "self"
    }
}

data class TicketDetail(val ticket: Ticket, val notification: String?)

data class CustomerCard(val customer: CustomerEmail, val tickets: List<Ticket>)

data class Conversation(val customer: CustomerEmail, val replies: List<Reply>)

sealed interface Actor {
    data class Agent(val id: ViewerId) : Actor
    data class Customer(val email: CustomerEmail) : Actor
    data object System : Actor
}

data class AgentsModel(val known: Set<ViewerId>, val online: Set<ViewerId>, val open: Map<ViewerId, Int>)

data class AgentPage(val agent: ViewerId, val before: Cursor?)

data class AgentModel(val assigned: List<Ticket>, val facts: Rows<Fact>, val numbers: Map<TicketId, Int>)

data class NavModel(val counts: Counts, val open: Map<ViewerId, Int>, val online: Set<ViewerId>)