package org.http4k.connect.amazon.ses

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/ses/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface SES {
    operator fun <R> invoke(action: SESAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("email")
}
