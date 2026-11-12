package helpdesk.store

import helpdesk.domain.CustomerEmail
import helpdesk.domain.FactId
import helpdesk.domain.ReplyBody
import helpdesk.domain.ReplyId
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import helpdesk.domain.ViewerId
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.http4k.fanbelt.withFanbeltMappings

object HelpdeskJson : ConfigurableMoshi(
    standardConfig()
        .withFanbeltMappings()
        .value(TicketId)
        .value(ReplyId)
        .value(FactId)
        .value(Subject)
        .value(CustomerEmail)
        .value(ReplyBody)
        .value(ViewerId)
        .done()
)