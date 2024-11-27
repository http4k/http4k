package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.core.Uri
import java.time.ZonedDateTime

import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteQueue(
    val QueueUrl: Uri,
    val expires: ZonedDateTime? = null,
) : SQSAction<Unit, Unit>("DeleteQueue", Unit::class, { })
