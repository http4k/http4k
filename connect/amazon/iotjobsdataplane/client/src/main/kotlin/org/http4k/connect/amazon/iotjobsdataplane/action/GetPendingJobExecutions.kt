package org.http4k.connect.amazon.iotjobsdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneMoshi
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

/** Gets every job execution for the thing which is not in a terminal state. */
@Http4kConnectAction
data class GetPendingJobExecutions(
    val thingName: ThingName,
) : IotJobsDataPlaneAction<PendingJobExecutions> {

    override fun toRequest() = Request(GET, Uri.of("").path("/things/${thingName.value}/jobs"))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotJobsDataPlaneMoshi.asA<PendingJobExecutions>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class PendingJobExecutions(
    val inProgressJobs: List<JobExecutionSummary> = emptyList(),
    val queuedJobs: List<JobExecutionSummary> = emptyList(),
)

@JsonSerializable
data class JobExecutionSummary(
    val jobId: JobId,
    val queuedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val versionNumber: Long? = null,
    val executionNumber: Long? = null,
)
