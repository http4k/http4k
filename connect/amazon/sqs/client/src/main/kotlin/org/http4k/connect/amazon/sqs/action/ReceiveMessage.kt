package org.http4k.connect.amazon.sqs.action

import com.squareup.moshi.Json
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.SQSMessage
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import java.time.ZonedDateTime

@Http4kConnectAction
@JsonSerializable
data class ReceiveMessage(
    @Json(name = "QueueUrl") val queueUrl: Uri,
    @Json(name = "MaxNumberOfMessages") val maxNumberOfMessages: Int? = null,
    @Json(name = "VisibilityTimeout") val visibilityTimeout: Int? = null,
    val expires: ZonedDateTime? = null,
    @Json(name = "WaitTimeSeconds") val waitTimeSeconds: Int? = null,
    @Json(name = "MessageAttributeNames") val messageAttributes: List<String>? = null,
    @Json(name = "EeceiveRequestAttemptId") val receiveRequestAttemptId: String? = null,
    @Json(name = "AttributeNames") val attributeNames: List<String>? = null,
) : SQSAction<List<SQSMessage>, ReceiveMessageResponse>("ReceiveMessage", ReceiveMessageResponse::class, { it.Messages.orEmpty() })

@JsonSerializable
data class ReceiveMessageResponse(
    val Messages: List<SQSMessage>?
)
