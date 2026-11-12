package helpdesk.web

import helpdesk.domain.Status
import helpdesk.domain.TicketId
import helpdesk.domain.ViewerId
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.enum
import org.http4k.lens.value
import org.http4k.fanbelt.store.Cursor

val ticketId = Path.value(TicketId).of("ticket")
val agentId = Path.value(ViewerId).of("agent")
val before = Query.value(Cursor).optional("before")
val status = Path.enum<Status>().of("status")
val providerState = Path.enum<ProviderState>().of("state")