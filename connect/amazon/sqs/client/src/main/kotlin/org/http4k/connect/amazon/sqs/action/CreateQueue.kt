package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.core.Uri
import java.time.ZonedDateTime
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateQueue(
    val QueueName: QueueName,
    val Tags: Map<String, String>? = null,
    val Attributes: Map<String, String>? = null,
) : SQSAction<CreatedQueue, CreatedQueue>("CreateQueue", CreatedQueue::class, {it}) {

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
