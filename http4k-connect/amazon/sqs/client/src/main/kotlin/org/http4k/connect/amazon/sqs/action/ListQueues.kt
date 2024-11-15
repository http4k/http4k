package org.http4k.connect.amazon.sqs.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.sqs.SQSAction
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListQueues(
    val MaxResults: Int? = null,
    val NextToken: String? = null,
    val QueueNamePrefix: String? = null
) : SQSAction<List<Uri>, ListQueuesResponse>("ListQueues", ListQueuesResponse::class, { it.QueueUrls })

@JsonSerializable
data class ListQueuesResponse(
    val NextToken: String? = null,
    val QueueUrls: List<Uri>
)
