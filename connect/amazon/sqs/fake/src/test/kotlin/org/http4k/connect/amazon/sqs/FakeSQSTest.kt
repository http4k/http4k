package org.http4k.connect.amazon.sqs

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class FakeSQSTest : SQSContract, FakeAwsContract {
    override val http = FakeSQS()

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
}
