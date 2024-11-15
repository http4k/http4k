package org.http4k.connect.amazon.sqs.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class QueueNameTest {
    @Test
    fun `can create from an SQS queue Uri`() {
        assertThat(
            QueueName.parse(Uri.of("https://sqs.us-east-2.amazonaws.com/123456789012/MyQueue")),
            equalTo(QueueName.of("MyQueue"))
        )
    }
}
