package org.http4k.connect.amazon.sqs.action

import dev.forkhandles.result4k.Result4k
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import java.time.ZonedDateTime

@Http4kConnectAction
@JsonSerializable
data class CreateQueue(
    val QueueName: QueueName,
    val Tags: Map<String, String>? = null,
    val Attributes: Map<String, String>? = null,
) : SQSAction<CreatedQueue, CreatedQueue>("CreateQueue", CreatedQueue::class, {it}), Action<Result4k<CreatedQueue, RemoteFailure>> {

    constructor(
        queueName: QueueName,
        tags: List<Tag>? = null,
        attributes: Map<String, String>? = null,
        expires: ZonedDateTime? = null
    ): this(
        QueueName = queueName,
        Tags = tags?.associate { it.Key to it.Value },
        Attributes = attributes
    )
}

@JsonSerializable
data class CreatedQueue(
    val QueueUrl: Uri
)
