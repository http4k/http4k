package org.http4k.connect.amazon.iotjobsdataplane

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * The device side of AWS IoT Jobs.
 *
 * Docs: https://docs.aws.amazon.com/iot/latest/apireference/API_Operations_AWS_IoT_Jobs_Data_Plane.html
 *
 * The companion carries the SigV4 SIGNING name (`iot-jobs-data`), which differs from the
 * endpoint host (`data.jobs.iot.<region>.amazonaws.com`). Signing correctness is
 * non-negotiable, so the companion holds the signing name and [Http] derives the endpoint
 * from the region instead of from the companion.
 */
@Http4kConnectApiClient
interface IotJobsDataPlane {
    operator fun <R> invoke(action: IotJobsDataPlaneAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("iot-jobs-data")
}
