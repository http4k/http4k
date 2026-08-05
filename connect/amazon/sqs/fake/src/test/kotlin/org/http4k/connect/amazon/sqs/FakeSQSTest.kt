package org.http4k.connect.amazon.sqs

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.sqs.MessageMD5ChecksumInterceptor.calculateMd5
import org.http4k.connect.amazon.sqs.action.SendMessageBatchEntry
import org.http4k.connect.amazon.sqs.model.MessageAttribute
import org.http4k.connect.amazon.sqs.model.MessageSystemAttribute
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.AWSTraceHeader
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.All
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.MessageDeduplicationId
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.MessageGroupId
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.SenderId
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.SentTimestamp
import org.http4k.connect.amazon.sqs.model.MessageSystemAttributeName.SequenceNumber
import org.http4k.connect.amazon.sqs.model.QueueName
import org.http4k.connect.successValue
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.junit.jupiter.api.Test
import java.time.Duration

class FakeSQSTest : SQSContract, FakeAwsContract {
    private val clock = TestClock()

    override val http = FakeSQS(clock = clock)

    private val fifoQueueName get() = QueueName.of("${uuid()}.fifo")
    private val fifoAttributes = mapOf("FifoQueue" to "true")
    private val traceHeader = "Root=1-5f4a4a2d-b94f96db34d41be1349080d2"

    @Test
    fun `multiple messages are handled correctly`() {
        with(sqs) {
            val created = createQueue(
                queueName,
                listOf(Tag("tag", "value")),
                mapOf("MaximumMessageSize" to "10000"),
                expires
            ).successValue()

            try {
                val id = sendMessage(created.QueueUrl, "hello world").successValue().MessageId
                val id1 = sendMessage(created.QueueUrl, "hello world 2").successValue().MessageId
                sendMessage(created.QueueUrl, "shouldn't be returned").successValue().MessageId

                val messages = receiveMessage(
                    created.QueueUrl,
                    maxNumberOfMessages = 2,
                    waitTimeSeconds = 10
                ).successValue()
                assertThat(messages.size, equalTo(2))
                assertThat(messages[0].messageId, equalTo(id))
                assertThat(messages[1].messageId, equalTo(id1))
            } finally {
                deleteQueue(created.QueueUrl, expires).successValue()
            }
        }
    }

    @Test
    fun `FakeSQS listMessages helper`() {
        with(sqs) {
            val created = createQueue(queueName).successValue()

            sendMessage(created.QueueUrl, "hi").successValue()
            sendMessage(created.QueueUrl, "by").successValue()

            val messages = http.listMessages(queueName)
            assertThat(messages, hasSize(equalTo(2)))
            assertThat(messages[0].body, equalTo("hi"))
            assertThat(messages[1].body, equalTo("by"))
        }
    }

    @Test
    fun `a send records the FIFO fields it was given`() {
        val name = fifoQueueName
        val created = sqs.createQueue(name, attributes = fifoAttributes).successValue()

        // no DelaySeconds here: AWS rejects a per-message delay on a FIFO queue
        val sent = sqs.sendMessage(
            queueUrl = created.QueueUrl,
            payload = "hello world",
            deduplicationId = "dedup-1",
            messageGroupId = "group-1",
            systemAttributes = listOf(MessageSystemAttribute(AWSTraceHeader, traceHeader, DataType.String))
        ).successValue()

        assertThat(sent.SequenceNumber, present(equalTo("00000000000000000001")))
        assertThat(sent.MD5OfMessageBody, equalTo("hello world".md5()))
        assertThat(sent.MD5OfMessageAttributes, absent())
        assertThat(
            sent.MD5OfMessageSystemAttributes,
            equalTo(calculateMd5(listOf(MessageAttribute(AWSTraceHeader, traceHeader, DataType.String))))
        )

        val stored = http.listMessages(name).single()
        assertThat(
            stored.systemAttributes, equalTo(
                mapOf(
                    SenderId to "001234567890",
                    SentTimestamp to "0",
                    MessageGroupId to "group-1",
                    MessageDeduplicationId to "dedup-1",
                    SequenceNumber to "00000000000000000001",
                    AWSTraceHeader to traceHeader
                )
            )
        )
    }

    @Test
    fun `received attributes are limited to those the request selects`() {
        val created = sqs.createQueue(fifoQueueName, attributes = fifoAttributes).successValue()

        sqs.sendMessage(
            created.QueueUrl, "hello world",
            deduplicationId = "dedup-1",
            messageGroupId = "group-1"
        ).successValue()

        assertThat(
            sqs.receiveMessage(created.QueueUrl).successValue().single().systemAttributes,
            equalTo(emptyMap())
        )

        assertThat(
            sqs.receiveMessage(created.QueueUrl, attributeNames = listOf(MessageGroupId))
                .successValue().single().systemAttributes,
            equalTo(mapOf(MessageGroupId to "group-1"))
        )

        val all = sqs.receiveMessage(created.QueueUrl, messageSystemAttributeNames = listOf(All))
            .successValue().single()

        assertThat(
            all.systemAttributes.keys,
            equalTo(setOf(SenderId, SentTimestamp, MessageGroupId, MessageDeduplicationId, SequenceNumber))
        )
    }

    @Test
    fun `a repeated deduplication id inside the window is accepted but not enqueued`() {
        val name = fifoQueueName
        val created = sqs.createQueue(name, attributes = fifoAttributes).successValue()

        val first = sqs.sendMessage(
            created.QueueUrl, "hello world",
            deduplicationId = "dedup-1", messageGroupId = "group-1"
        ).successValue()

        clock.tickBy(DeduplicationInterval.minusMillis(1))

        val second = sqs.sendMessage(
            created.QueueUrl, "a different body",
            deduplicationId = "dedup-1", messageGroupId = "group-1"
        ).successValue()

        // the duplicate is accepted against the original message ...
        assertThat(second.MessageId, equalTo(first.MessageId))
        assertThat(second.SequenceNumber, equalTo(first.SequenceNumber))

        // ... but SQS digests the request it has just received, not the one it deduplicated against
        assertThat(second.MD5OfMessageBody, equalTo("a different body".md5()))
        assertThat(second.MD5OfMessageBody, !equalTo(first.MD5OfMessageBody))

        assertThat(http.listMessages(name).map { it.body }, equalTo(listOf("hello world")))
    }

    @Test
    fun `a queue with ContentBasedDeduplication deduplicates on the body`() {
        val name = fifoQueueName
        val created = sqs.createQueue(
            name,
            attributes = fifoAttributes + mapOf("ContentBasedDeduplication" to "true")
        ).successValue()

        val first = sqs.sendMessage(created.QueueUrl, "hello world", messageGroupId = "group-1").successValue()
        val repeat = sqs.sendMessage(created.QueueUrl, "hello world", messageGroupId = "group-1").successValue()
        sqs.sendMessage(created.QueueUrl, "a different body", messageGroupId = "group-1").successValue()

        assertThat(repeat.MessageId, equalTo(first.MessageId))
        assertThat(
            http.listMessages(name).map { it.body },
            equalTo(listOf("hello world", "a different body"))
        )
    }

    @Test
    fun `a FIFO send with no deduplication id is rejected unless the queue derives one`() {
        val created = sqs.createQueue(fifoQueueName, attributes = fifoAttributes).successValue()

        assertThat(
            sqs.sendMessage(created.QueueUrl, "hello world", messageGroupId = "group-1")
                .failureOrNull()?.status,
            equalTo(BAD_REQUEST)
        )
    }

    @Test
    fun `a repeated deduplication id is enqueued again once the window has passed`() {
        val name = fifoQueueName
        val created = sqs.createQueue(name, attributes = fifoAttributes).successValue()

        val first = sqs.sendMessage(
            created.QueueUrl, "hello world",
            deduplicationId = "dedup-1", messageGroupId = "group-1"
        ).successValue()

        clock.tickBy(DeduplicationInterval)

        val second = sqs.sendMessage(
            created.QueueUrl, "hello again",
            deduplicationId = "dedup-1", messageGroupId = "group-1"
        ).successValue()

        assertThat(second.MessageId, !equalTo(first.MessageId))
        assertThat(second.SequenceNumber, present(equalTo("00000000000000000002")))
        assertThat(http.listMessages(name).map { it.body }, equalTo(listOf("hello world", "hello again")))
    }

    @Test
    fun `a repeated deduplication id does not extend the window`() {
        val name = fifoQueueName
        val created = sqs.createQueue(name, attributes = fifoAttributes).successValue()

        sqs.sendMessage(created.QueueUrl, "hello world", deduplicationId = "dedup-1", messageGroupId = "group-1").successValue()

        clock.tickBy(Duration.ofMinutes(4))
        sqs.sendMessage(created.QueueUrl, "hello world", deduplicationId = "dedup-1", messageGroupId = "group-1").successValue()

        clock.tickBy(Duration.ofMinutes(2))
        sqs.sendMessage(created.QueueUrl, "hello again", deduplicationId = "dedup-1", messageGroupId = "group-1").successValue()

        assertThat(http.listMessages(name).map { it.body }, equalTo(listOf("hello world", "hello again")))
    }

    @Test
    fun `a standard queue records the deduplication id but never deduplicates`() {
        val created = sqs.createQueue(queueName).successValue()

        val first = sqs.sendMessage(
            created.QueueUrl, "hello world",
            deduplicationId = "dedup-1", messageGroupId = "group-1"
        ).successValue()
        val second = sqs.sendMessage(
            created.QueueUrl, "hello world",
            deduplicationId = "dedup-1", messageGroupId = "group-1"
        ).successValue()

        assertThat(second.MessageId, !equalTo(first.MessageId))
        assertThat(first.SequenceNumber, absent())

        val stored = http.listMessages(queueName)
        assertThat(stored, hasSize(equalTo(2)))
        assertThat(stored.first().systemAttributes[MessageDeduplicationId], equalTo("dedup-1"))
        assertThat(stored.first().systemAttributes[MessageGroupId], equalTo("group-1"))
        assertThat(stored.first().systemAttributes[SequenceNumber], absent())
    }

    @Test
    fun `a batch send deduplicates within a FIFO queue`() {
        val name = fifoQueueName
        val created = sqs.createQueue(name, attributes = fifoAttributes).successValue()

        val (first, second) = sqs.sendMessageBatch(
            created.QueueUrl,
            listOf(
                SendMessageBatchEntry("one", "hello world", deduplicationId = "dedup-1", messageGroupId = "group-1"),
                SendMessageBatchEntry("two", "hello again", deduplicationId = "dedup-1", messageGroupId = "group-1")
            )
        ).successValue()

        assertThat(second.MessageId, equalTo(first.MessageId))
        assertThat(first.SequenceNumber, present(equalTo("00000000000000000001")))

        // the deduplicated entry still reports the checksum of the body it carried
        assertThat(second.MD5OfMessageBody, equalTo("hello again".md5()))

        assertThat(http.listMessages(name).map { it.body }, equalTo(listOf("hello world")))
    }

    @Test
    fun `deleting a queue forgets its deduplication history`() {
        val name = fifoQueueName
        val created = sqs.createQueue(name, attributes = fifoAttributes).successValue()
        sqs.sendMessage(created.QueueUrl, "hello world", deduplicationId = "dedup-1", messageGroupId = "group-1").successValue()

        // on the live queue the same id is deduplicated away, so the delete is what makes the difference
        sqs.sendMessage(created.QueueUrl, "hello world", deduplicationId = "dedup-1", messageGroupId = "group-1").successValue()
        assertThat(http.listMessages(name), hasSize(equalTo(1)))

        sqs.deleteQueue(created.QueueUrl).successValue()

        val recreated = sqs.createQueue(name, attributes = fifoAttributes).successValue()
        sqs.sendMessage(recreated.QueueUrl, "hello world", deduplicationId = "dedup-1", messageGroupId = "group-1").successValue()

        assertThat(http.listMessages(name), hasSize(equalTo(1)))
    }
}
