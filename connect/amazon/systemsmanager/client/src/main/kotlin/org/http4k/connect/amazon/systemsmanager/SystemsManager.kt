package org.http4k.connect.amazon.systemsmanager

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/systems-manager/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface SystemsManager {
    operator fun <R : Any> invoke(action: SystemsManagerAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("ssm")
}
