package org.http4k.connect.amazon.sqs.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import java.time.ZonedDateTime

@Http4kConnectAction
@JsonSerializable
data class DeleteQueue(
    val QueueUrl: Uri,
    val expires: ZonedDateTime? = null,
) : SQSAction<Unit, Unit>("DeleteQueue", Unit::class, { }), Action<Result4k<Unit, RemoteFailure>>
