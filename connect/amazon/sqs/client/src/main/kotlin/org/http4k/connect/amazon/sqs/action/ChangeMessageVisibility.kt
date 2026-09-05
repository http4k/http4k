package org.http4k.connect.amazon.sqs.action

import com.squareup.moshi.Json
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import java.time.ZonedDateTime

/** A [visibilityTimeout] of 0 returns the message to the queue immediately. */
@Http4kConnectAction
@JsonSerializable
data class ChangeMessageVisibility(
    @Json(name = "QueueUrl") val queueUrl: Uri,
    @Json(name = "ReceiptHandle") val receiptHandle: ReceiptHandle,
    @Json(name = "VisibilityTimeout") val visibilityTimeout: Int,
    val expires: ZonedDateTime? = null
) : SQSAction<Unit, Unit>("ChangeMessageVisibility", Unit::class, { }), Action<Result<Unit, RemoteFailure>>

@JsonSerializable
data class ChangeMessageVisibilityData(
    val QueueUrl: Uri,
    val ReceiptHandle: ReceiptHandle,
    val VisibilityTimeout: Int
)
