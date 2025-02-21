package org.http4k.connect.amazon.ses

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.base64Encode
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.ses.model.Body
import org.http4k.connect.amazon.ses.model.Content
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.EmailContent
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.RawMessage
import org.http4k.connect.amazon.ses.model.RawMessageBase64
import org.http4k.connect.successValue
import org.http4k.filter.debug
import org.junit.jupiter.api.Test

interface SESContract : AwsContract {
    private val ses
        get() =
        SES.Http(aws.region, aws::credentials, http.debug())

    val from get() = EmailAddress.of("source@example.com")
    val to get() = EmailAddress.of("destination@example.com")

    @Test
    fun `send simple email`() {
        val response = ses.sendEmail(
            fromEmailAddress = from,
            destination = Destination(
                toAddresses = setOf(to )
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

        assertEmailSent()
    }

    @Test
    fun `send raw email`() {
        val response = ses.sendEmail(
            fromEmailAddress = from,
            destination = Destination(
                toAddresses = setOf(to)
            ),
            content = EmailContent(
                raw = RawMessage(
                    data = RawMessageBase64.of(sampleMimeMessage(from).base64Encode())
                )
            )
        )

        assertThat(response.successValue(), present())

        assertEmailSent()
    }

    fun assertEmailSent()
}
