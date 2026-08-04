package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.model.PayloadFormatIndicator
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.MemoryBody
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Moshi
import org.http4k.lens.Header.CONTENT_TYPE

@Http4kConnectAction
data class Publish(
    val topic: TopicName,
    val payload: ByteArray,
    val qos: Int? = null,
    val retain: Boolean? = null,
    val messageExpiry: Long? = null,
    val responseTopic: String? = null,
    val contentType: String? = null,
    val correlationData: Base64Blob? = null,
    val payloadFormatIndicator: PayloadFormatIndicator? = null,
    val userProperties: List<Pair<String, String>>? = null,
) : IotDataPlaneAction<Unit> {

    override fun toRequest() = queryParameters()
        .fold(topicRequest()) { request, (name, value) -> request.query(name, value) }
        .headers(mqtt5Headers())
        .with(CONTENT_TYPE of OCTET_STREAM)
        .body(MemoryBody(payload))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }

    /**
     * The topic goes into the path exactly as given. The AWS auth filter percent-encodes the path
     * afterwards, so encoding it here as well would put %252F on the wire and AWS would read the
     * escape as part of the topic name.
     */
    private fun topicRequest() = Request(POST, Uri.of("").path("/topics/${topic.value}"))

    private fun queryParameters() = listOf(
        "contentType" to contentType,
        "messageExpiry" to messageExpiry?.toString(),
        "qos" to qos?.toString(),
        "responseTopic" to responseTopic,
        "retain" to retain?.toString()
    ).present()

    private fun mqtt5Headers() = listOf(
        "x-amz-mqtt5-correlation-data" to correlationData?.value,
        "x-amz-mqtt5-payload-format-indicator" to payloadFormatIndicator?.name,
        "x-amz-mqtt5-user-properties" to userProperties?.toHeaderValue()
    ).present()

    /** ByteArray equality is by identity, so equals/hashCode have to compare the payload here. */
    private fun fieldsOtherThanPayload() = listOf(
        topic, qos, retain, messageExpiry, responseTopic,
        contentType, correlationData, payloadFormatIndicator, userProperties
    )

    override fun equals(other: Any?) = other is Publish &&
        payload.contentEquals(other.payload) &&
        fieldsOtherThanPayload() == other.fieldsOtherThanPayload()

    override fun hashCode() = 31 * fieldsOtherThanPayload().hashCode() + payload.contentHashCode()
}

private fun List<Pair<String, String?>>.present() = filterNot { (_, value) -> value == null }

/**
 * AWS expects the user properties as base64 of a JSON array of single-entry objects, eg.
 * `[{"deviceName": "alpha"}, {"deviceCnt": "45"}]`.
 */
private fun List<Pair<String, String>>.toHeaderValue() =
    Base64Blob.encode(Moshi.asFormatString(map { (key, value) -> mapOf(key to value) })).value
