package org.http4k.connect.amazon.sqs.model

import se.ansman.kotshi.JsonSerializable
import com.squareup.moshi.Json
import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.core.model.MessageFieldsDto
import org.http4k.connect.model.Base64Blob

@JsonSerializable
data class SQSMessage(
    @Json(name = "MessageId") val messageId: SQSMessageId,
    @Json(name = "Body") val body: String,
    @Json(name = "MD5OfBody") val md5OfBody: String,
    @Json(name = "ReceiptHandle") val receiptHandle: ReceiptHandle,
    @Json(name = "MessageAttributes") val messageAttributes: Map<String, MessageFieldsDto> = emptyMap()
) {
    constructor(
        messageId: SQSMessageId,
        body: String,
        md5OfBody: String,
        receiptHandle: ReceiptHandle,
        attributes: List<MessageAttribute>
    ): this(
        messageId = messageId,
        body = body,
        md5OfBody = md5OfBody,
        receiptHandle = receiptHandle,
        messageAttributes = attributes.associate { it.name to it.toDto() }
    )

    val attributes get() = messageAttributes.map { (name, value) -> value.toSqs(name) }
}

internal fun MessageFieldsDto.toSqs(name: String) = when(dataType) {
    DataType.String, DataType.Number -> if (stringListValues != null) {
        MessageAttribute(name, stringListValues.orEmpty(), dataType)
    } else {
        MessageAttribute(name, stringValue!!, dataType)
    }
    DataType.Binary -> if (binaryListValues != null) {
        MessageAttribute(name, binaryListValues.orEmpty().map(Base64Blob::of))
    } else {
        MessageAttribute(name, Base64Blob.of(binaryValue!!))
    }
}
