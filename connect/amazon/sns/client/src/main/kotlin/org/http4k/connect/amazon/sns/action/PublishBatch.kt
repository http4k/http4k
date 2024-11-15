package org.http4k.connect.amazon.sns.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.children
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.asList
import org.http4k.connect.amazon.core.sequenceOfNodes
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.sns.SNSAction
import org.http4k.connect.amazon.sns.model.MessageAttribute
import org.http4k.connect.amazon.sns.model.SNSMessageId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Response
import org.w3c.dom.Node

@Http4kConnectAction
data class PublishBatch(
    val TopicArn: ARN,
    val PublishBatchRequestEntries: List<PublishBatchRequestEntry>
) : SNSAction<PublishBatchResult>(
    "PublishBatch",
    *PublishBatchRequestEntries.flatMapIndexed { index, entry -> entry.toParams(index + 1) }.toTypedArray(),
    "TopicArn" to TopicArn.value
) {
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(PublishBatchResult.from(response))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

data class PublishBatchRequestEntry(
    val Id: String,
    val Message: String,
    val MessageAttributes: List<MessageAttribute>? = null,
    val MessageDeduplicationId: String? = null,
    val MessageGroupId: String? = null,
    val MessageStructure: String? = null,
    val Subject: String? = null
)

private fun PublishBatchRequestEntry.toParams(index: Int) = buildList {
    add("PublishBatchRequestEntries.member.$index.Id" to Id)
    add("PublishBatchRequestEntries.member.$index.Message" to Message)
    asList(MessageAttributes.orEmpty())
        .map { (name, value) -> "PublishBatchRequestEntries.member.$index.$name" to value }
        .let { addAll(it) }
    if (MessageDeduplicationId != null) {
        add("PublishBatchRequestEntries.member.$index.MessageDeduplicationId" to MessageDeduplicationId)
    }
    if (MessageGroupId != null) {
        add("PublishBatchRequestEntries.member.$index.MessageGroupId" to MessageGroupId)
    }
    if (MessageStructure != null) {
        add("PublishBatchRequestEntries.member.$index.MessageStructure" to MessageStructure)
    }
    if (Subject != null) {
        add("PublishBatchRequestEntries.member.$index.Subject" to Subject)
    }
}

data class PublishBatchResult(
    val Failed: List<BatchResultErrorEntry>,
    val Succesful: List<PublishBatchResultEntry>
) {
    companion object {
        fun from(response: Response): PublishBatchResult {
            val doc = response.xmlDoc()

            return PublishBatchResult(
                Succesful = doc.getElementsByTagName("Successful")
                    .sequenceOfNodes()
                    .flatMap { it.children("member") }
                    .map { it.toResultEntry() }
                    .toList(),
                Failed = doc.getElementsByTagName("Failed")
                    .sequenceOfNodes()
                    .flatMap { it.children("member") }
                    .map { it.toErrorEntry() }
                    .toList()
            )
        }
    }
}

data class BatchResultErrorEntry(
    val Code: String,
    val Id: String,
    val SenderFault: Boolean,
    val Message: String?
)

private fun Node.toErrorEntry() = BatchResultErrorEntry(
    Code = firstChildText("Code")!!,
    Id = firstChildText("Id")!!,
    SenderFault = firstChild("SenderFault")!!.text().toBoolean(),
    Message = firstChildText("Message")
)

data class PublishBatchResultEntry(
    val Id: String?,
    val MessageId: SNSMessageId?,
    val SequenceNumber: String?
)

private fun Node.toResultEntry() = PublishBatchResultEntry(
    Id = firstChildText("Id"),
    MessageId = firstChildText("MessageId")?.let(SNSMessageId::parse),
    SequenceNumber = firstChildText("SequenceNumber")
)
