package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.RawMessage
import se.ansman.kotshi.JsonSerializable

data class EmailMessage(
    val source: EmailAddress,
    val destination: Destination,
    val message: Message?,
    val rawMessage: RawMessage?
)

@JsonSerializable
data class SESError(val message: String?)
