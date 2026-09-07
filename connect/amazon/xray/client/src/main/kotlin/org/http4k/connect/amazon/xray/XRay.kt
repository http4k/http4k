package org.http4k.connect.amazon.xray

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * The AWS X-Ray read APIs, of which the trace retrieval operations are implemented.
 *
 * Docs: https://docs.aws.amazon.com/xray/latest/api/Welcome.html
 */
@Http4kConnectApiClient
interface XRay {
    operator fun <R> invoke(action: XRayAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("xray")
}
