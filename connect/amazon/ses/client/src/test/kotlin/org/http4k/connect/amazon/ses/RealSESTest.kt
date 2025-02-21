package org.http4k.connect.amazon.ses

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.ses.model.EmailAddress

class RealSESTest : SESContract, RealAwsContract {
    override val http = JavaHttpClient()

    // Need to be very careful which addresses you use with real ses
    override val from = System.getenv("http4k-development-ses-from")?.let(EmailAddress::of) ?: super.from
    override val to = (System.getenv("http4k-development-ses-to")?.let(EmailAddress::of) ?: super.to)

    override fun assertEmailSent() {
        // no-op
    }
}
