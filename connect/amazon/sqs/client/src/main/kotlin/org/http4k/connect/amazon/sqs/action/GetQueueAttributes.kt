package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import com.squareup.moshi.Json

@Http4kConnectAction
@JsonSerializable
data class GetQueueAttributes(
    @Json(name = "QueueUrl") val queueUrl: Uri,
    @Json(name = "AttributeNames") val attributes: List<String>? = null,
) : SQSAction<QueueAttributes, QueueAttributes>("GetQueueAttributes", QueueAttributes::class, { it })

@JsonSerializable
data class QueueAttributes(
    @Json(name = "Attributes") val attributes: Map<String, String> = emptyMap()
)
