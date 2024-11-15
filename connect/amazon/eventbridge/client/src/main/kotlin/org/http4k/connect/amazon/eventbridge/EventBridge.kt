package org.http4k.connect.amazon.eventbridge

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/eventbridge/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface EventBridge {
    operator fun <R : Any> invoke(action: EventBridgeAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("events")
}
