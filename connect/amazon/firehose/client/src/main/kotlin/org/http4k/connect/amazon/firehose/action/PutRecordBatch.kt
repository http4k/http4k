package org.http4k.connect.amazon.firehose.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.firehose.FirehoseAction
import org.http4k.connect.amazon.firehose.FirehoseMoshi
import org.http4k.connect.amazon.model.DeliveryStreamName
import org.http4k.connect.amazon.model.Record
import org.http4k.connect.amazon.model.RequestResponses
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class PutRecordBatch(
    val DeliveryStreamName: DeliveryStreamName,
    val Records: List<Record>
) : FirehoseAction<BatchResult>(BatchResult::class, FirehoseMoshi)

@JsonSerializable
data class BatchResult(
    val Encrypted: Boolean,
    val FailedPutCount: Int,
    val RequestResponses: List<RequestResponses>?
)
