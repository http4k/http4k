@file:Suppress("FunctionName")

package org.http4k.connect.amazon.ses

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.ses.action.SendEmail
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.SESMessageId
import org.http4k.connect.storage.Storage
import java.util.Properties
import org.http4k.connect.amazon.ses.action.SendEmailResponse as SendEmailResponseDto

fun AwsRestJsonFake.SendEmail(messagesBySender: Storage<List<EmailMessage>>) = route<SendEmail> fn@{ data ->
    val rawMessage = data.content.raw?.let { raw ->
        val session = Session.getDefaultInstance(Properties())
        raw.data.decodedInputStream().use {
            MimeMessage(session, it)
        }
    }

    val source = data.fromEmailAddress ?: rawMessage?.getFromAddress() ?: return@fn Failure(SESError(null))
    val existing = messagesBySender[source.value] ?: emptyList()

    val emailMessage = EmailMessage(
        source = source,
        destination = data.destination,
        message = data.content.simple,
        rawMessage = data.content.raw
    )

    messagesBySender[source.value] = existing + emailMessage

    Success(SendEmailResponseDto(SESMessageId.of(emailMessage.toString())))
}

private fun MimeMessage.getFromAddress() = from.toList()
    .filterIsInstance<InternetAddress>()
    .first().toHttp4k()

private fun MimeMessage.getDestination() = Destination(
    toAddresses = getRecipients(Message.RecipientType.TO).orEmpty()
        .filterIsInstance<InternetAddress>()
        .map { it.toHttp4k() }
        .toSet().takeIf { it.isNotEmpty() },
    ccAddresses = getRecipients(Message.RecipientType.CC).orEmpty()
        .filterIsInstance<InternetAddress>()
        .map { it.toHttp4k() }
        .toSet().takeIf { it.isNotEmpty() },
    bccAddresses = getRecipients(Message.RecipientType.BCC).orEmpty()
        .filterIsInstance<InternetAddress>()
        .map { it.toHttp4k() }
        .toSet().takeIf { it.isNotEmpty() },
)

private fun InternetAddress.toHttp4k() = personal?.trim()?.takeIf { it.isNotEmpty() }
    ?.let { personal -> EmailAddress.parse("\"${personal.replace("\"", "\\\"")}\" <$address>") }
    ?: EmailAddress.parse(address)
