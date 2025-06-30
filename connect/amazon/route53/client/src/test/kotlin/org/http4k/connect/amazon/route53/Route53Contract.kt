package org.http4k.connect.amazon.route53

import org.http4k.connect.amazon.AwsContract
import org.junit.jupiter.api.Test

interface Route53Contract: AwsContract {

    val route53 get() = Route53.Http({ aws.credentials }, http)

    @Test
}
