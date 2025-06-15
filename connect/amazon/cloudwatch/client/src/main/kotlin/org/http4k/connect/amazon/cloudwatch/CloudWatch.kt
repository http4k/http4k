package org.http4k.connect.amazon.cloudwatch

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface CloudWatch {
    operator fun <R : Any> invoke(action: CloudWatchAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("monitoring")
}
