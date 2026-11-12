package helpdesk.store

import helpdesk.domain.Fact
import helpdesk.domain.FactId
import helpdesk.domain.KnownAgent
import helpdesk.domain.Org
import helpdesk.domain.Outbox
import helpdesk.domain.Reply
import helpdesk.domain.ReplyId
import helpdesk.domain.Ticket
import helpdesk.domain.TicketId
import helpdesk.domain.ViewerId
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke

class HelpdeskTables(tables: Tables) {
    val org = tables<String, Org>(ORGS)
    val tickets = tables<TicketId, Ticket>(TICKETS)
    val replies = tables<ReplyId, Reply>(REPLIES)
    val journal = tables<FactId, Fact>(JOURNAL)
    val agents = tables<ViewerId, KnownAgent>(AGENTS)
    val outbox = tables<TicketId, Outbox>(OUTBOX)

    companion object {
        val TICKETS = TableName.of("tickets")
        val REPLIES = TableName.of("replies")
        val JOURNAL = TableName.of("journal")
        val AGENTS = TableName.of("agents")
        val ORGS = TableName.of("org")
        val OUTBOX = TableName.of("outbox")
    }
}