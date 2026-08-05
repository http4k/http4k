package org.http4k.connect.amazon.sqs

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.asResultOr
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.peek
import org.http4k.connect.amazon.AwsRestJsonFake
import org.http4k.connect.amazon.RestfulError
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.MessageFieldsDto
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sqs.action.CreateQueue
import org.http4k.connect.amazon.sqs.action.CreatedQueue
import org.http4k.connect.amazon.sqs.action.DeleteMessageBatch
import org.http4k.connect.amazon.sqs.action.DeleteMessageBatchResponse
import org.http4k.connect.amazon.sqs.action.DeleteMessageBatchResultEntry
import org.http4k.connect.amazon.sqs.action.DeleteMessageData
import org.http4k.connect.amazon.sqs.action.DeleteQueue
import org.http4k.connect.amazon.sqs.action.GetQueueAttributes
import org.http4k.connect.amazon.sqs.action.ListQueues
import org.http4k.connect.amazon.sqs.action.ListQueuesResponse
import org.http4k.connect.amazon.sqs.action.QueueAttributes
import org.http4k.connect.amazon.sqs.action.ReceiveMessage
import org.http4k.connect.amazon.sqs.action.ReceiveMessageResponse
import org.http4k.connect.amazon.sqs.action.SendMessage
import org.http4k.connect.amazon.sqs.action.SendMessageBatch
import org.http4k.connect.amazon.sqs.action.SendMessageBatchResponse
import org.http4k.connect.amazon.sqs.action.SendMessageBatchResultEntry
import org.http4k.connect.amazon.sqs.action.SentMessage
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.All
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.MessageDeduplicationId
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.MessageGroupId
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.SenderId
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.SentTimestamp
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.SequenceNumber
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.connect.amazon.sqs.model.SQSMessage
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.routing.asRouter
import org.http4k.routing.bind
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

private fun forAction(name: String) = { r: Request ->
    r.method == Method.POST && r.header("X-Amz-Target") == "AmazonSQS.$name"
}.asRouter()

fun AwsRestJsonFake.createQueue(
    queues: Storage<List<SQSMessage>>,
    queueConfig: Storage<QueueConfig>,
    awsAccount: AwsAccount
) =
    forAction("CreateQueue") bind route<CreateQueue> { data ->
        if (queues.keySet(data.QueueName.value).isEmpty()) {
            queues[data.QueueName.value] = listOf()
        }

        queueConfig[data.QueueName.value] = QueueConfig(
            contentBasedDeduplication = data.Attributes
                ?.get("ContentBasedDeduplication").toBoolean()
        )

        Success(CreatedQueue(uri.extend(Uri.of("/$awsAccount/${data.QueueName}"))))
    }

fun AwsRestJsonFake.getQueueAttributes(queues: Storage<List<SQSMessage>>) =
    forAction("GetQueueAttributes") bind route<GetQueueAttributes> { data ->
        val name = data.queueUrl.queueName()

        queues[name]
            .asResultOr { queueNotFound(name) }
            .map { queue ->
                QueueAttributes(mapOf(
                    "LastModifiedTimestamp" to "0",
                    "CreatedTimestamp" to "0",
                    "MessageRetentionPeriod" to "0",
                    "DelaySeconds" to "0",
                    "ReceiveMessageWaitTimeSeconds" to "0",
                    "MaximumMessageSize" to "0",
                    "VisibilityTimeout" to "0",
                    "ApproximateNumberOfMessagesDelayed" to queue.size.toString(),
                    "ApproximateNumberOfMessages" to queue.size.toString(),
                    "ApproximateNumberOfMessagesNotVisible" to "0"
                ))
            }
    }

fun AwsRestJsonFake.listQueues(region: Region, account: AwsAccount, queues: Storage<List<SQSMessage>>) =
    forAction("ListQueues") bind route<ListQueues> {
        // TODO handle pagination
        Success(ListQueuesResponse(
            NextToken = null,
            QueueUrls = queues.keySet().map { Uri.of("https://sqs.$region.amazonaws.com/$account/$it") }
        ))
    }

fun AwsRestJsonFake.deleteQueue(
    queues: Storage<List<SQSMessage>>,
    deduplication: Storage<DeduplicationRecord> = Storage.InMemory(),
    queueConfig: Storage<QueueConfig> = Storage.InMemory()
) =
    forAction("DeleteQueue") bind route<DeleteQueue> { data ->
        val queueName = data.QueueUrl.queueName()
        queues[queueName]
            .asResultOr { queueNotFound(queueName) }
            .peek {
                queues -= queueName
                queueConfig -= queueName
                deduplication.removeAll(queueName.deduplicationKeyPrefix())
            }
            .map { }
    }

fun AwsRestJsonFake.sendMessage(
    queues: Storage<List<SQSMessage>>,
    deduplication: Storage<DeduplicationRecord> = Storage.InMemory(),
    queueConfig: Storage<QueueConfig> = Storage.InMemory(),
    clock: Clock = Clock.systemUTC(),
    sequenceNumbers: AtomicLong = AtomicLong()
) =
    forAction("SendMessage") bind route<SendMessage> fn@{ data ->
        val name = data.queueUrl.queueName()

        if (queues[name] == null) return@fn Failure(queueNotFound(name))

        invalidFifoSend(name, data.messageDeduplicationId, data.messageGroupId, queueConfig[name])
            ?.let { return@fn Failure(it) }

        queues[name].asResultOr { queueNotFound(name) }.map {
            val sent = send(
                queues, deduplication, queueConfig, clock, sequenceNumbers, name,
                data.messageBody, data.messageDeduplicationId, data.messageGroupId,
                data.messageAttributes, data.messageSystemAttributes
            )

            SentMessage(
                MessageId = sent.messageId,
                SequenceNumber = sent.sequenceNumber,
                MD5OfMessageBody = sent.md5OfBody,
                MD5OfMessageAttributes = sent.md5OfMessageAttributes,
                MD5OfMessageSystemAttributes = sent.md5OfMessageSystemAttributes
            )
        }
    }

fun AwsRestJsonFake.sendMessageBatch(
    queues: Storage<List<SQSMessage>>,
    deduplication: Storage<DeduplicationRecord> = Storage.InMemory(),
    queueConfig: Storage<QueueConfig> = Storage.InMemory(),
    clock: Clock = Clock.systemUTC(),
    sequenceNumbers: AtomicLong = AtomicLong()
) =
    forAction("SendMessageBatch") bind route<SendMessageBatch> fn@{ data ->
        val queueName = data.queueUrl.queueName()
        if (queues[queueName] == null) return@fn Failure(queueNotFound(queueName))

        data.entries.firstNotNullOfOrNull {
            invalidFifoSend(queueName, it.MessageDeduplicationId, it.MessageGroupId, queueConfig[queueName])
        }?.let { return@fn Failure(it) }

        Success(SendMessageBatchResponse(
            Failed = null,
            Successful = data.entries.map { entry ->
                val sent = send(
                    queues, deduplication, queueConfig, clock, sequenceNumbers, queueName,
                    entry.MessageBody, entry.MessageDeduplicationId, entry.MessageGroupId,
                    entry.MessageAttributes, entry.MessageSystemAttributes
                )

                SendMessageBatchResultEntry(
                    Id = entry.Id,
                    MessageId = sent.messageId,
                    MD5OfMessageBody = sent.md5OfBody,
                    MD5OfMessageAttributes = sent.md5OfMessageAttributes,
                    SequenceNumber = sent.sequenceNumber,
                    MD5OfMessageSystemAttributes = sent.md5OfMessageSystemAttributes
                )
            }
        ))
    }

fun AwsRestJsonFake.receiveMessage(queues: Storage<List<SQSMessage>>) =
    forAction("ReceiveMessage") bind route<ReceiveMessage> { data ->
        val name = data.queueUrl.queueName()

        queues[name].asResultOr { queueNotFound(name) }.map { queue ->
            val messagesToSend = data.maxNumberOfMessages?.let { queue.take(it) } ?: queue
            val selected = data.attributeNames.orEmpty() + data.messageSystemAttributeNames.orEmpty()
            ReceiveMessageResponse(messagesToSend.map { it.asDelivered(selected) })
        }
    }

private class SendResult(
    val messageId: SQSMessageId,
    val sequenceNumber: String?,
    val md5OfBody: String,
    val md5OfMessageAttributes: String?,
    val md5OfMessageSystemAttributes: String?
)

/** A duplicate on a FIFO queue is not enqueued, and reports the original message's identity. */
private fun AwsRestJsonFake.send(
    queues: Storage<List<SQSMessage>>,
    deduplication: Storage<DeduplicationRecord>,
    queueConfig: Storage<QueueConfig>,
    clock: Clock,
    sequenceNumbers: AtomicLong,
    queueName: String,
    body: String,
    deduplicationId: String?,
    groupId: String?,
    messageAttributes: Map<String, MessageFieldsDto>?,
    systemAttributes: Map<String, MessageFieldsDto>?
): SendResult {
    val sentAt = clock.instant()
    val fifo = queueName.isFifoQueue()

    // SQS reports a content-derived id to consumers as the message's own MessageDeduplicationId
    val effectiveDeduplicationId = deduplicationId
        .orContentBased(body, queueConfig[queueName])
        ?.takeIf { fifo }

    val deduplicationKey = effectiveDeduplicationId?.let { queueName.deduplicationKeyPrefix() + it }

    // SQS digests the request it has just received, whether or not it deduplicates it away
    fun resultOf(record: DeduplicationRecord) = SendResult(
        messageId = record.messageId,
        sequenceNumber = record.sequenceNumber,
        md5OfBody = body.md5(),
        md5OfMessageAttributes = messageAttributes.orEmpty().md5OfFields(),
        md5OfMessageSystemAttributes = systemAttributes.orEmpty().md5OfFields()
    )

    val deduplicated = deduplicationKey
        ?.let(deduplication::get)
        ?.takeIf { Duration.between(it.sentAt, sentAt) < DeduplicationInterval }

    if (deduplicated != null) return resultOf(deduplicated)

    // only FIFO queues are given a SequenceNumber, so only they consume one
    val sequenceNumber = if (fifo) sequenceNumbers.incrementAndGet().toString().padStart(20, '0') else null

    val message = SQSMessage(
        messageId = SQSMessageId.of(UUID.randomUUID().toString()),
        body = body,
        md5OfBody = body.md5(),
        receiptHandle = ReceiptHandle.of(UUID.randomUUID().toString()),
        messageAttributes = messageAttributes.orEmpty(),
        systemAttributes = sentAttributes(
            sentAt, sequenceNumber, effectiveDeduplicationId ?: deduplicationId, groupId, systemAttributes
        )
    )

    queues[queueName] = queues[queueName].orEmpty() + message

    val record = DeduplicationRecord(message.messageId, sequenceNumber, sentAt)
    deduplicationKey?.let { deduplication[it] = record }

    return resultOf(record)
}

private fun AwsRestJsonFake.sentAttributes(
    sentAt: Instant,
    sequenceNumber: String?,
    deduplicationId: String?,
    groupId: String?,
    systemAttributes: Map<String, MessageFieldsDto>?
) = buildMap {
    put(SenderId, accountId.value)
    put(SentTimestamp, sentAt.toEpochMilli().toString())
    groupId?.let { put(MessageGroupId, it) }
    deduplicationId?.let { put(MessageDeduplicationId, it) }
    sequenceNumber?.let { put(SequenceNumber, it) }
    systemAttributes.orEmpty().forEach { (name, value) -> value.stringValue?.let { put(name, it) } }
}

/** SQS reports only the attributes the request asked for. */
private fun SQSMessage.asDelivered(selected: List<String>) = copy(
    systemAttributes = when {
        selected.isEmpty() -> emptyMap()
        selected.contains(All) -> systemAttributes
        else -> systemAttributes.filterKeys { it in selected }
    }
)

// queue names cannot contain '/', so this cannot collide with another queue's records
private fun String.deduplicationKeyPrefix() = "$this/"

fun AwsRestJsonFake.deleteMessage(queues: Storage<List<SQSMessage>>) =
    forAction("DeleteMessage") bind route<DeleteMessageData> { data ->
        val name = data.QueueUrl.queueName()
        val receiptHandle = data.ReceiptHandle

        queues[name]
            .asResultOr { queueNotFound(name) }
            .peek { queue -> queues[name] = queue.filterNot { it.receiptHandle == receiptHandle } }
            .map { }
    }

fun AwsRestJsonFake.deleteMessageBatch(queues: Storage<List<SQSMessage>>) =
    forAction("DeleteMessageBatch") bind route<DeleteMessageBatch> fn@{ data ->
        val queueName = data.queueUrl.queueName()
        val queue = queues[queueName] ?: return@fn Failure(queueNotFound(queueName))

        val toDelete = data.entries.mapNotNull { entry ->
            queue.find { it.receiptHandle == entry.ReceiptHandle }
        }.toSet()

        queues[queueName] = queue - toDelete

        Success(DeleteMessageBatchResponse(
            Failed = emptyList(),
            Successful = toDelete.map {
                DeleteMessageBatchResultEntry(it.messageId)
            }
        ))
    }

private fun Uri.queueName() = toString().queueName()
private fun String.queueName() = substring(lastIndexOf('/') + 1)

/**
 * A FIFO queue with ContentBasedDeduplication set derives the deduplication id from the body when
 * the send did not carry one of its own.
 */
private fun String?.orContentBased(body: String, config: QueueConfig?) =
    this ?: body.sha256().takeIf { config?.contentBasedDeduplication == true }

/**
 * SQS rejects a FIFO send which carries no MessageDeduplicationId unless the queue derives one from
 * the body itself.
 */
private fun AwsRestJsonFake.invalidFifoSend(
    name: String,
    deduplicationId: String?,
    groupId: String?,
    config: QueueConfig?
): RestfulError? {
    if (!name.isFifoQueue()) return null

    val message = when {
        groupId == null -> "The request must contain the parameter MessageGroupId"

        deduplicationId == null && config?.contentBasedDeduplication != true ->
            "The queue should either have ContentBasedDeduplication enabled or MessageDeduplicationId provided explicitly"

        else -> return null
    }

    return RestfulError(Status(400, ""), message, ARN.of(awsService, region, accountId, name), "queue")
}

private fun AwsRestJsonFake.queueNotFound(name: String): RestfulError {
    val resourceArn = ARN.of(awsService, region, accountId, name)
    val message = "Queue $name not found"
    return RestfulError(Status(404, ""), message, resourceArn, "queue")
}
