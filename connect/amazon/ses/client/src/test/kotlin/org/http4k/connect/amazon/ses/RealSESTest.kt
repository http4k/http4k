package org.http4k.connect.amazon.ses

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.ses.model.EmailAddress

class RealSESTest : SESContract, RealAwsContract {
    override val http = JavaHttpClient()

    // Need to be very careful which addresses you use with real ses
    override val from = EmailAddress.of(System.getenv("http4k-development-ses-from"))
    override val to = EmailAddress.of(System.getenv("http4k-development-ses-to"))

    override fun assertEmailSent() {
        // no-op
    }
}
