package org.http4k.connect.amazon.sns.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.asList
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.textOptional
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.sns.SNSAction
import org.http4k.connect.amazon.sns.model.MessageAttribute
import org.http4k.connect.amazon.sns.model.PhoneNumber
import org.http4k.connect.amazon.sns.model.SNSMessageId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response

@Http4kConnectAction
data class PublishMessage(
    val message: String,
    val subject: String? = null,
    val phoneNumber: PhoneNumber? = null,
    val topicArn: ARN? = null,
    val targetArn: ARN? = null,
    val messageDeduplicationId: String? = null,
    val messageGroupId: String? = null,
    val messageStructure: String? = null,
    val attributes: List<MessageAttribute>? = null
) : SNSAction<PublishedMessage>(
    "Publish",
    *(
        asList(attributes ?: emptyList()) +
            listOfNotNull(
                "Message" to message,
                messageStructure?.let { "MessageStructure" to it },
                messageDeduplicationId?.let { "MessageDeduplicationId" to it },
                messageGroupId?.let { "MessageGroupId" to it },
                subject?.let { "Subject" to it },
                topicArn?.let { "TopicArn" to it.value },
                targetArn?.let { "TargetArn" to it.value },
                phoneNumber?.let { "PhoneNumber" to it.value },
            )
        ).toTypedArray()
) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(PublishedMessage.from(response))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

data class PublishedMessage(
    val MessageId: SNSMessageId,
    val SequenceNumber: String? = null
) {
    companion object {
        fun from(response: Response) =
            with(response.xmlDoc()) {
                PublishedMessage(
                    SNSMessageId.of(text("MessageId")),
                    textOptional("SequenceNumber")
                )
            }
    }
}

