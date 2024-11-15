package org.http4k.connect.amazon.apprunner

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/apprunner/latest/api/Welcome.html
 */
@Http4kConnectApiClient
interface AppRunner {
    operator fun <RESP : Any> invoke(action: AppRunnerAction<RESP>): Result<RESP, RemoteFailure>

    companion object : AwsServiceCompanion("apprunner")
}

