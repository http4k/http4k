package org.http4k.connect.amazon.ses.action

import com.squareup.moshi.Json
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.ses.SESAction
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.EmailContent
import org.http4k.connect.amazon.ses.model.MessageTag
import org.http4k.connect.amazon.ses.model.SESMessageId
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class SendEmail(
    @Json(name = "Content") val content: EmailContent,
    @Json(name = "Destination") val destination: Destination? = null,
    @Json(name = "FromEmailAddress") val fromEmailAddress: EmailAddress? = null,
    @Json(name = "ConfigurationSetName") val configurationSetName: String? = null,
    @Json(name = "EmailTags") val emailTags: List<MessageTag>? = null,
    @Json(name = "ReplyToAddresses") val replyToAddresses: List<EmailAddress>? = null

) : SESAction<SendEmailResponse>(SendEmailResponse::class, Uri.of("/v2/email/outbound-emails"))

@JsonSerializable
data class SendEmailResponse(
    @Json(name = "MessageId") val messageId: SESMessageId
)
