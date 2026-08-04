package org.http4k.connect.amazon.iotdataplane

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/iot/latest/apireference/API_Operations_AWS_IoT_Data_Plane.html
 */
@Http4kConnectApiClient
interface IotDataPlane {
    operator fun <R> invoke(action: IotDataPlaneAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("iotdata")
}
