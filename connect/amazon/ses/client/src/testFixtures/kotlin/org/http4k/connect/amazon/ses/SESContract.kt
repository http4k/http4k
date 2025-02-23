package org.http4k.connect.amazon.ses

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Failure
import org.http4k.base64Encode
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.ses.model.Body
import org.http4k.connect.amazon.ses.model.Content
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.EmailContent
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.RawMessage
import org.http4k.connect.amazon.ses.model.RawMessageBase64
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Method.POST
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
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
                        text = Content("text stuff", Charsets.UTF_8),
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

    @Test
    fun `send email - without destination`() {
        val response = ses.sendEmail(
            fromEmailAddress = from,
            content = EmailContent(
                simple = Message(
                    subject = Content("important stuff"),
                    body = Body(
                        text = Content("text stuff", Charsets.UTF_8),
                        html = Content("html stuff")
                    )
                )
            )
        )

        response.failureValue {
            assertThat(it.status, equalTo(BAD_REQUEST))
            assertThat(it.method, equalTo(POST))
            assertThat(it.uri, equalTo(Uri.of("/v2/email/outbound-emails")))
        }
    }

    @Test
    fun `send email - without from address`() {
        val response = ses.sendEmail(
            destination = Destination(
                toAddresses = setOf(to)
            ),
            content = EmailContent(
                simple = Message(
                    subject = Content("important stuff"),
                    body = Body(
                        text = Content("text stuff", Charsets.UTF_8),
                        html = Content("html stuff")
                    )
                )
            )
        )

        response.failureValue {
            assertThat(it.status, equalTo(BAD_REQUEST))
            assertThat(it.method, equalTo(POST))
            assertThat(it.uri, equalTo(Uri.of("/v2/email/outbound-emails")))
        }
    }

    fun assertEmailSent()
}
