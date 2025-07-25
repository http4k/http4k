package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.core.Uri
import java.time.ZonedDateTime
import com.squareup.moshi.Json
import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteMessage(
    @Json(name = "QueueUrl") val queueUrl: Uri,
    @Json(name = "ReceiptHandle") val receiptHandle: ReceiptHandle,
    val expires: ZonedDateTime? = null
) : SQSAction<Unit, Unit>("DeleteMessage", Unit::class, { }), Action<Result<Unit, RemoteFailure>>

@JsonSerializable
data class DeleteMessageData(
    val QueueUrl: Uri,
    val ReceiptHandle: ReceiptHandle
)
