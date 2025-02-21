package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.RawMessage

data class EmailMessage(
    val source: EmailAddress,
    val destination: Destination,
    val message: Message?,
    val rawMessage: RawMessage?
)
