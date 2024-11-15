package org.http4k.connect.amazon.cloudfront

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.routing.routes
import java.time.Clock
import java.time.Clock.systemUTC


class FakeCloudFront(
    private val clock: Clock = systemUTC()
) : ChaoticHttpHandler() {

    override val app = routes(
        CreateInvalidation(clock)
    )

    /**
     * Convenience function to get CloudFront client
     */
    fun client() = CloudFront.Http({ AwsCredentials("accessKey", "secret") }, this, clock)
}

fun main() {
    FakeCloudFront().start()
}
