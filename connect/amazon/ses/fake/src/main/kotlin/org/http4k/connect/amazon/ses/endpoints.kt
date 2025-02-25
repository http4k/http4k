@file:Suppress("FunctionName")

package org.http4k.connect.amazon.ses

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.ses.action.SendEmail
import org.http4k.connect.amazon.ses.model.SESMessageId
import org.http4k.connect.storage.Storage
import org.http4k.connect.amazon.ses.action.SendEmailResponse as SendEmailResponseDto

fun AwsRestJsonFake.SendEmail(messagesBySender: Storage<List<EmailMessage>>) = route<SendEmail> fn@{ data ->
    val source = data.fromEmailAddress ?: return@fn Failure(SESError(null))
    val existing = messagesBySender[source.value] ?: emptyList()

    val emailMessage = EmailMessage(
        source = source,
        destination = data.destination ?: return@fn Failure(SESError(null)),
        message = data.content.simple,
        rawMessage = data.content.raw
    )

    messagesBySender[source.value] = existing + emailMessage

    Success(SendEmailResponseDto(SESMessageId.of(emailMessage.toString())))
}
