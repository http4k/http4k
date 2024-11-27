package org.http4k.connect.amazon.sns

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.DataType.Number
import org.http4k.connect.amazon.sns.action.PublishBatchRequestEntry
import org.http4k.connect.amazon.sns.model.MessageAttribute
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

val topics = Storage.InMemory<List<SNSMessage>>()

class FakeSNSTest : SNSContract, FakeAwsContract {
    override val http = FakeSNS(topics)

    @AfterEach
    fun clean() {
        topics.removeAll()
    }

    @Test
    fun `topic messages are parsed`() {
        with(sns) {
            val topicArn = createTopic(topicName, listOf(), mapOf()).successValue().topicArn
            try {
                val attributes = listOf(
                    MessageAttribute("foo", "123", Number),
                    MessageAttribute("bar", "123", Number),
                    MessageAttribute("binaryfoo", Base64Blob.encode("foobar"))
                )
                publishMessage(
                    "hello world", "subject", topicArn = topicArn,
                    attributes = attributes
                ).successValue()

                assertThat(topics[topicName.value]!![0].message, equalTo("hello world"))
                assertThat(topics[topicName.value]!![0].subject, equalTo("subject"))
                assertThat(topics[topicName.value]!![0].attributes.first().name, equalTo("foo"))
                assertThat(topics[topicName.value]!![0].attributes.first().value, equalTo("123"))
                assertThat(topics[topicName.value]!![0].attributes.first().dataType, equalTo(Number))
            } finally {
                deleteTopic(topicArn).successValue()
            }
        }
    }

    @Test
    fun `batch publish messages are parsed`() {
        with(sns) {
            val topicArn = createTopic(topicName, listOf(), mapOf()).successValue().topicArn
            try {
                val attributes = listOf(
                    MessageAttribute("foo", "123", Number),
                    MessageAttribute("bar", "123", Number),
                    MessageAttribute("binaryfoo", Base64Blob.encode("foobar"))
                )

                val result = publishBatch(
                    TopicArn = topicArn,
                    PublishBatchRequestEntries = listOf(
                        PublishBatchRequestEntry(
                            Id = "message1",
                            Subject = "subject",
                            Message = "hello world",
                            MessageAttributes = attributes
                        )
                    )
                ).successValue()

                assertThat(result.Succesful.first().Id, equalTo("message1"))

                val message = topics[topicName.value]!![0]
                assertThat(message.subject, equalTo("subject"))
                assertThat(message.message, equalTo("hello world"))
                assertThat(message.attributes.first().name, equalTo("foo"))
                assertThat(message.attributes.first().value, equalTo("123"))
                assertThat(message.attributes.first().dataType, equalTo(Number))
            } finally {
                deleteTopic(topicArn).successValue()
            }
        }
    }
}
