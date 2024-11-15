package org.http4k.connect.amazon.sns

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.sns.action.PublishBatchResultEntry
import org.http4k.connect.amazon.sns.model.SNSMessageId
import org.http4k.template.ViewModel

object DeleteTopicResponse : ViewModel
data class CreateTopicResponse(val topicArn: ARN) : ViewModel
data class ListTopicsResponse(val arns: List<ARN>) : ViewModel
data class PublishResponse(val messageId: SNSMessageId) : ViewModel
data class PublishBatchResponse(val results: List<PublishBatchResultEntry>) : ViewModel

