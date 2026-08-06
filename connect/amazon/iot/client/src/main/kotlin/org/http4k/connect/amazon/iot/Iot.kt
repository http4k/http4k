package org.http4k.connect.amazon.iot

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * The AWS IoT control plane, of which the Jobs and stream operations are implemented.
 *
 * Docs: https://docs.aws.amazon.com/iot/latest/apireference/Welcome.html
 *
 * The companion name is both the SigV4 signing name and the endpoint prefix: the current
 * AWS-published service model (aws.auth#sigv4 name "iot") signs the control plane as `iot`,
 * which is also the host prefix of `iot.<region>.amazonaws.com`. Older SDKs signed this
 * service as `execute-api`, and the API reference prose still says so - but current botocore
 * (signingName "iot") and the aws-sdk-go-v2 Smithy model agree on `iot`, so it is what every
 * shipping SDK sends and what AWS accepts.
 */
@Http4kConnectApiClient
interface Iot {
    operator fun <R> invoke(action: IotAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("iot")
}
