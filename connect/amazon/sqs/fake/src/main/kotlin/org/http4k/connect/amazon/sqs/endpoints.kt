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
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.connect.amazon.sqs.model.SQSMessage
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.extend
import org.http4k.routing.Predicate
import org.http4k.routing.bind
import java.util.UUID

private fun forAction(name: String) = Predicate("", Status.NOT_FOUND, { r: Request ->
    r.method == Method.POST && r.header("X-Amz-Target") == "AmazonSQS.$name"
})

fun AwsRestJsonFake.createQueue(queues: Storage<List<SQSMessage>>, awsAccount: AwsAccount) =
    forAction("CreateQueue") bind route<CreateQueue> { data ->
        if (queues.keySet(data.QueueName.value).isEmpty()) {
            queues[data.QueueName.value] = listOf()
        }

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
            QueueUrls = queues.keySet().map { Uri.of("https://sqs.${region}.amazonaws.com/${account}/$it") }
        ))
    }

fun AwsRestJsonFake.deleteQueue(queues: Storage<List<SQSMessage>>) =
    forAction("DeleteQueue") bind route<DeleteQueue> { data ->
        val queueName = data.QueueUrl.queueName()
        queues[queueName]
            .asResultOr { queueNotFound(queueName) }
            .peek { queues -= queueName }
            .map {  }
    }

fun AwsRestJsonFake.sendMessage(queues: Storage<List<SQSMessage>>) =
    forAction("SendMessage") bind route<SendMessage> { data ->
        val name = data.queueUrl.queueName()

        queues[name].asResultOr { queueNotFound(name) }.map { queue ->
            val messageId = SQSMessageId.of(UUID.randomUUID().toString())
            val receiptHandle = ReceiptHandle.of(UUID.randomUUID().toString())

            val sqsMessage = SQSMessage(messageId, data.messageBody, data.messageBody.md5(), receiptHandle, data.messageAttributes.orEmpty())
            queues[name] = queue + sqsMessage

            SentMessage(
                MessageId = sqsMessage.messageId,
                SequenceNumber = null,
                MD5OfMessageBody = sqsMessage.md5OfBody,
                MD5OfMessageAttributes = if (sqsMessage.attributes.isNotEmpty()) sqsMessage.md5OfAttributes() else null
            )
        }
    }

fun AwsRestJsonFake.sendMessageBatch(queues: Storage<List<SQSMessage>>) =
    forAction("SendMessageBatch") bind route<SendMessageBatch> fn@{ data ->
        val queueName = data.queueUrl.queueName()
        val queue = queues[queueName] ?: return@fn Failure(queueNotFound(queueName))

        val results = data.entries.map { entry ->
            val message = SQSMessage(
                messageId = SQSMessageId.of(UUID.randomUUID().toString()),
                body = entry.MessageBody,
                md5OfBody = entry.MessageBody.md5(),
                receiptHandle = ReceiptHandle.of(UUID.randomUUID().toString()),
                messageAttributes = entry.MessageAttributes.orEmpty()
            )

            val result = SendMessageBatchResultEntry(
                Id = entry.Id,
                MessageId = message.messageId,
                MD5OfMessageBody = message.md5OfBody(),
                MD5OfMessageAttributes = if (message.attributes.isNotEmpty()) message.md5OfAttributes() else null
            )

            message to result
        }

        queues[queueName] = queue + results.map { it.first }

        Success(SendMessageBatchResponse(
            Failed = null,
            Successful = results.map { it.second }
        ))
    }

fun AwsRestJsonFake.receiveMessage(queues: Storage<List<SQSMessage>>) =
    forAction("ReceiveMessage") bind route<ReceiveMessage> { data ->
        val name = data.queueUrl.queueName()

        queues[name].asResultOr { queueNotFound(name) }.map { queue ->
            val messagesToSend = data.maxNumberOfMessages?.let { queue.take(it) } ?: queue
            ReceiveMessageResponse(messagesToSend)
        }
    }

fun AwsRestJsonFake.deleteMessage(queues: Storage<List<SQSMessage>>) =
    forAction("DeleteMessage") bind route<DeleteMessageData> { data ->
        val name = data.QueueUrl.queueName()
        val receiptHandle = data.ReceiptHandle

        queues[name]
            .asResultOr { queueNotFound(name) }
            .peek { queue -> queues[name] = queue.filterNot { it.receiptHandle == receiptHandle } }
            .map {  }
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

private fun AwsRestJsonFake.queueNotFound(name: String): RestfulError {
    val resourceArn = ARN.of(awsService, region, accountId, name)
    val message = "Queue $name not found"
    return RestfulError(Status(404, ""), message, resourceArn, "queue")
}
