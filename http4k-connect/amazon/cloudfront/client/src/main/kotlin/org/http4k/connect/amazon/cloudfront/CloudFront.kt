package org.http4k.connect.amazon.cloudfront

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/cloudfront/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface CloudFront {
    operator fun <R> invoke(action: CloudFrontAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("cloudfront")
}
