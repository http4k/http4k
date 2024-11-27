package org.http4k.connect.amazon.ses

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.ses.model.Body
import org.http4k.connect.amazon.ses.model.Destination
import org.http4k.connect.amazon.ses.model.EmailAddress
import org.http4k.connect.amazon.ses.model.Message
import org.http4k.connect.amazon.ses.model.Subject
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.filter.debug
import org.junit.jupiter.api.Test

interface SESContract : AwsContract {
    private val ses
        get() =
        SES.Http(aws.region, aws::credentials, http.debug())

    val from get() = EmailAddress.of("source@example.com")

    @Test
    fun `sends emails`() {
        val response = ses.sendEmail(
            source = from,
            destination = Destination(
                toAddresses = setOf(
                    EmailAddress.of("destination@example.com")
                )
            ),
            message = Message(
                subject = Subject.of("Hello"),
                html = Body.of("Hello World")
            )
        )

        assertThat(response.successValue(), present())

        assertEmailSent()
    }

    abstract fun assertEmailSent()
}
