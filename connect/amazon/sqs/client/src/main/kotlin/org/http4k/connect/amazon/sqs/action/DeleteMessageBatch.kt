package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.BatchResultErrorEntry
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import com.squareup.moshi.Json

@Http4kConnectAction
@JsonSerializable
data class DeleteMessageBatch(
    @Json(name = "QueueUrl") val queueUrl: Uri,
    @Json(name = "Entries") val entries: List<DeleteMessageBatchEntry>,
) : SQSAction<List<SQSMessageId>, DeleteMessageBatchResponse>("DeleteMessageBatch", DeleteMessageBatchResponse::class, { it.Successful.map { it.Id } }) {

    constructor(
        queueUrl: Uri,
        entries: Collection<Pair<SQSMessageId, ReceiptHandle>>
    ): this(
        queueUrl = queueUrl,
        entries = entries.map { DeleteMessageBatchEntry(it.first, it.second) }
    )
}

@JsonSerializable
data class DeleteMessageBatchEntry(
    val Id: SQSMessageId,
    val ReceiptHandle: ReceiptHandle
)

@JsonSerializable
data class DeleteMessageBatchResponse(
    val Failed: List<BatchResultErrorEntry>?,
    val Successful: List<DeleteMessageBatchResultEntry>
)

@JsonSerializable
data class DeleteMessageBatchResultEntry(
    val Id: SQSMessageId
)
