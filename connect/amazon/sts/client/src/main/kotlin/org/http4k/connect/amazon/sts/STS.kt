package org.http4k.connect.amazon.sts

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/STS/latest/APIReference/welcome.html
 */
@Http4kConnectApiClient
interface STS {
    operator fun <R> invoke(action: STSAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("sts")
}
