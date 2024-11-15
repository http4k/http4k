package org.http4k.connect.amazon.sns

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/sns/latest/api/Welcome.html
 */
@Http4kConnectApiClient
interface SNS {
    operator fun <R> invoke(action: SNSAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("sns")
}
