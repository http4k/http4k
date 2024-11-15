package org.http4k.connect.amazon.cloudfront

import org.http4k.connect.amazon.FakeAwsContract

class FakeCloudFrontTest : CloudFrontContract, FakeAwsContract {
    override val http = FakeCloudFront()
}
