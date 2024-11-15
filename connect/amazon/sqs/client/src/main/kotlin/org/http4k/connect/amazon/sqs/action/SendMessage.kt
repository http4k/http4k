package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.MessageAttribute
import org.http4k.connect.amazon.sqs.model.MessageSystemAttribute
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import java.time.ZonedDateTime
import com.squareup.moshi.Json
import org.http4k.connect.amazon.core.model.MessageFieldsDto

@Http4kConnectAction
@JsonSerializable
data class SendMessage(
    @Json(name = "QueueUrl") val queueUrl: Uri,
    @Json(name = "MessageBody") val messageBody: String,
    @Json(name = "DelaySeconds") val delaySeconds: Int? = null,
    @Json(name = "MessageDeDuplicationId") val messageDeduplicationId: String? = null,
    @Json(name = "MessageGroupId") val messageGroupId: String? = null,
    @Json(name = "MessageAttributes") val messageAttributes: Map<String, MessageFieldsDto>? = null,
    @Json(name = "MessageSystemAttributes") val messageSystemAttributes: Map<String, MessageFieldsDto>? = null
) : SQSAction<SentMessage, SentMessage>("SendMessage", SentMessage::class, { it} ) {
    constructor(
        queueUrl: Uri,
        payload: String,
        delaySeconds: Int? = null,
        deduplicationId: String? = null,
        messageGroupId: String? = null,
        expires: ZonedDateTime? = null,
        attributes: List<MessageAttribute>? = null,
        systemAttributes: List<MessageSystemAttribute>? = null
    ): this(
        queueUrl = queueUrl,
        messageBody = payload,
        delaySeconds = delaySeconds,
        messageDeduplicationId = deduplicationId,
        messageGroupId = messageGroupId,
        messageAttributes = attributes?.associate { it.name to it.toDto() },
        messageSystemAttributes = systemAttributes?.associate { it.name to it.toDto() }
    )
}

@JsonSerializable
data class SentMessage(
    val MD5OfMessageBody: String,
    val MessageId: SQSMessageId,
    val MD5OfMessageAttributes: String? = null,
    val SequenceNumber: String? = null
)
