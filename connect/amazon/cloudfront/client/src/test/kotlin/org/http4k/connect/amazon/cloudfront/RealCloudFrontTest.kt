package org.http4k.connect.amazon.cloudfront

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract

class RealCloudFrontTest : CloudFrontContract, RealAwsContract {
    override val http = JavaHttpClient()
}
