package org.http4k.connect.amazon.sns

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isEmpty
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.DataType.Number
import org.http4k.connect.amazon.core.model.DataType.String
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.sns.action.PublishBatchRequestEntry
import org.http4k.connect.amazon.sns.model.MessageAttribute
import org.http4k.connect.amazon.sns.model.TopicName
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.Test
import java.util.UUID

interface SNSContract : AwsContract {
    val sns
        get() =
        SNS.Http(aws.region, { aws.credentials }, http)

    val topicName get() = TopicName.of(uuid().toString())

    @Test
    fun `topic lifecycle`() {
        with(sns) {
            val topicArn = createTopic(
                topicName,
                listOf(Tag("key", "value"), Tag("key2", "value2")),
                mapOf("foo" to "bar")
            ).successValue().topicArn
            try {
                assertThat(listTopics().successValue().items.contains(topicArn), equalTo(true))

                publishMessage(
                    "hello world", "subject", topicArn = topicArn,
                    attributes = listOf(
                        MessageAttribute("foo", "123", Number),
                        MessageAttribute("bar", "123", Number),
                        MessageAttribute("binaryfoo", Base64Blob.encode("foobar"))
                    )
                ).successValue()

                val batchResult = publishBatch(
                    TopicArn = topicArn,
                    PublishBatchRequestEntries = listOf(
                        PublishBatchRequestEntry(
                            Id = "message1",
                            Subject = "serious stuff",
                            Message = "super serious",
                            MessageAttributes = listOf(
                                MessageAttribute("foo", "123", Number),
                                MessageAttribute("bar", "stuff", String)
                            )
                        ),
                        PublishBatchRequestEntry(
                            Id = "message2",
                            Message = "hello",
                            MessageAttributes = listOf(
                                MessageAttribute("foo", "123", Number)
                            )
                        )
                    )
                ).successValue()

                assertThat(batchResult.Failed, isEmpty)
                assertThat(batchResult.Succesful, hasSize(equalTo(2)))
                assertThat(batchResult.Succesful.map { it.Id }, equalTo(listOf("message1", "message2")))
            } finally {
                deleteTopic(topicArn).successValue()
            }
        }
    }
}
