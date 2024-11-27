package org.http4k.connect.amazon.sqs

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.DataType
import org.http4k.connect.amazon.sqs.model.MessageAttribute
import org.http4k.connect.amazon.sqs.model.ReceiptHandle
import org.http4k.connect.amazon.sqs.model.SQSMessage
import org.http4k.connect.amazon.sqs.model.SQSMessageId
import org.junit.jupiter.api.Test

class SQSMessageTest {

    @Test
    fun getMd5OfMessageBody() {
        assertThat(
            SQSMessage(
                SQSMessageId.of("123"), "helloworld", "helloworld".md5(),
                ReceiptHandle.of("123"), listOf()
            ).md5OfBody,
            equalTo("fc5e038d38a57032085441e7fe7010b0")
        )
    }

    @Test
    fun getMd5OfMessageAttributes() {
        assertThat(
            SQSMessage(
                SQSMessageId.of("123"), "helloworld", "helloworld".md5(),
                ReceiptHandle.of("123"), listOf(MessageAttribute("attr", "foobar", DataType.String))
            ).md5OfAttributes(),
            equalTo("b540361d137a5d88a70fc9323f9f69dc")
        )
    }
}
