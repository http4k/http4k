package org.http4k.connect.amazon.ses

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.present
import org.http4k.base64Encode
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.ses.model.Body
import org.http4k.connect.amazon.ses.model.Content
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.EmailContent
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.RawMessage
import org.http4k.connect.amazon.ses.model.RawMessageBase64
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class FakeSESTest : SESContract, FakeAwsContract {

    override val http = FakeSES()

    override fun assertEmailSent() {
        assertThat(http.messagesBySender[from.value]?.size, equalTo(1))
    }

    @Test
    fun `send and validate simple email`() {
        val response = http.client().sendEmail(
            fromEmailAddress = from,
            destination = Destination(
                toAddresses = setOf(to)
            ),
            content = EmailContent(
                simple = Message(
                    subject = Content("important stuff"),
                    body = Body(
                        text = Content("text stuff"),
                        html = Content("html stuff")
                    )
                )
            )
        )

        assertThat(response.successValue(), present())

        assertThat(http.messagesBySender[from.value]!!, hasElement(EmailMessage(
            source = from,
            destination = Destination(
                toAddresses = setOf(to)
            ),
            message = Message(
                subject = Content("important stuff"),
                body = Body(
                    html = Content("html stuff"),
                    text = Content("text stuff")
                )
            ),
            rawMessage = null
        )))
    }

    @Test
    fun `send and validate raw email`() {
        val response = http.client().sendEmail(
            fromEmailAddress = from,
            destination = Destination(
                toAddresses = setOf(to),
                ccAddresses = setOf(
                    EmailAddress.of("flyonthe@wall.com")
                )
            ),
            content = EmailContent(
                raw = RawMessage(
                    data = RawMessageBase64.of(sampleMimeMessage(from).base64Encode())
                )
            )
        )

        assertThat(response.successValue(), present())

        assertThat(http.messagesBySender[from.value]!!, hasElement(EmailMessage(
            source = from,
            destination = Destination(
                toAddresses = setOf(to),
                ccAddresses = setOf(EmailAddress.of("flyonthe@wall.com"))
            ),
            message = null,
            rawMessage = RawMessage(
                RawMessageBase64.of(sampleMimeMessage(from).base64Encode())
            )
        )))
    }
}
