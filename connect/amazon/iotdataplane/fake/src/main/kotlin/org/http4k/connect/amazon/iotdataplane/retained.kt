package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.amazon.iotdataplane.action.RetainedMessage
import org.http4k.connect.amazon.iotdataplane.action.RetainedMessageSummary
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.TimestampMillis
import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import java.time.Clock

/** AWS retains one message per topic, and treats a publish with an empty payload as a delete. */
internal fun Storage<RetainedMessage>.retain(
    topic: TopicName,
    payload: Base64Blob,
    request: Request,
    clock: Clock
) {
    when {
        payload.value.isEmpty() -> remove(topic.value)

        else -> this[topic.value] = RetainedMessage(
            topic = topic,
            lastModifiedTime = TimestampMillis.of(clock.instant()),
            qos = request.query("qos")?.toInt() ?: 0,
            payload = payload,
            userProperties = request.header("x-amz-mqtt5-user-properties")?.let(Base64Blob::of)
        )
    }
}

internal fun RetainedMessage.summary() = RetainedMessageSummary(
    topic = topic,
    lastModifiedTime = lastModifiedTime,
    payloadSize = payload?.decodedBytes()?.size?.toLong() ?: 0,
    qos = qos
)

/** The AWS SDK turns this status and error header into a ResourceNotFoundException. */
internal fun retainedMessageNotFound(topic: TopicName) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body("""{"message":"No retained message found for topic: '${topic.value}'"}""")
