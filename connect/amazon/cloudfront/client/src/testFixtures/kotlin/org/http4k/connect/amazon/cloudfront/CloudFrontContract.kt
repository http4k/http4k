package org.http4k.connect.amazon.cloudfront

import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.cloudfront.model.DistributionId
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

interface CloudFrontContract : AwsContract {
    private val cloudFront get() = CloudFront.Http({ aws.credentials }, http)

    private val distribution get() = DistributionId.of("E1HHLORGLBAQYP")

    @Test
    fun `invalidate cache`() {
        cloudFront.createInvalidation(distribution, "/foobar").successValue()
    }
}

