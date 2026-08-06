package org.http4k.connect.amazon.iotjobsdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneMoshi
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecution
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable

/**
 * Gets and starts the device's next pending job execution: a QUEUED pick transitions to
 * IN_PROGRESS. With nothing pending the service answers 200 with an empty document,
 * surfaced here as a null [StartedJobExecution.execution].
 *
 * The `$next` path segment is sent unencoded; the AWS auth filter percent-encodes the path
 * afterwards, so encoding it here as well would put %2524 on the wire.
 */
@Http4kConnectAction
data class StartNextPendingJobExecution(
    val thingName: ThingName,
    val statusDetails: Map<String, String>? = null,
    val stepTimeoutInMinutes: Long? = null,
) : IotJobsDataPlaneAction<StartedJobExecution> {

    override fun toRequest() = Request(PUT, Uri.of("").path("/things/${thingName.value}/jobs/\$next"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(IotJobsDataPlaneMoshi.asFormatString(StartNextPendingJobExecutionData(statusDetails, stepTimeoutInMinutes)))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotJobsDataPlaneMoshi.asA<StartedJobExecution>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class StartNextPendingJobExecutionData(
    val statusDetails: Map<String, String>? = null,
    val stepTimeoutInMinutes: Long? = null,
)

@JsonSerializable
data class StartedJobExecution(
    val execution: JobExecution? = null,
)
