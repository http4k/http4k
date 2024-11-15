package org.http4k.connect.amazon.cloudwatchlogs

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface CloudWatchLogs {
    operator fun <R : Any> invoke(action: CloudWatchLogsAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("logs")
}
