package org.http4k.connect.amazon.ses

import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.SESMessageId
import org.http4k.template.ViewModel

data class SendEmailResponse(val messageId: SESMessageId) : ViewModel

data class EmailMessage(
    val source: EmailAddress,
    val to: Set<EmailAddress>,
    val cc: Set<EmailAddress>,
    val bcc: Set<EmailAddress>,
    val message: Message
)
