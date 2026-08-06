package org.http4k.connect.amazon.iotjobsdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneMoshi
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecutionStatus
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable

/**
 * Updates the status of a job execution. A device may only set IN_PROGRESS, SUCCEEDED,
 * FAILED or REJECTED. When [expectedVersion] does not match the stored version the update
 * is rejected with 409; the AWS docs call this "a VersionMismatch error" but the service
 * model declares no such exception, so it surfaces under the operation's only declared 409,
 * InvalidStateTransitionException. When [statusDetails] is not specified the stored details
 * are unchanged; when specified they replace the stored map. [includeJobExecutionState]
 * and [includeJobDocument] both default to false on the server.
 */
@Http4kConnectAction
data class UpdateJobExecution(
    val thingName: ThingName,
    val jobId: JobId,
    val status: JobExecutionStatus,
    val statusDetails: Map<String, String>? = null,
    val expectedVersion: Long? = null,
    val includeJobExecutionState: Boolean? = null,
    val includeJobDocument: Boolean? = null,
    val executionNumber: Long? = null,
    val stepTimeoutInMinutes: Long? = null,
) : IotJobsDataPlaneAction<UpdatedJobExecution> {

    override fun toRequest() = Request(POST, Uri.of("").path("/things/${thingName.value}/jobs/${jobId.value}"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(
            IotJobsDataPlaneMoshi.asFormatString(
                UpdateJobExecutionData(
                    status = status,
                    statusDetails = statusDetails,
                    expectedVersion = expectedVersion,
                    includeJobExecutionState = includeJobExecutionState,
                    includeJobDocument = includeJobDocument,
                    executionNumber = executionNumber,
                    stepTimeoutInMinutes = stepTimeoutInMinutes,
                )
            )
        )

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotJobsDataPlaneMoshi.asA<UpdatedJobExecution>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class UpdateJobExecutionData(
    val status: JobExecutionStatus,
    val statusDetails: Map<String, String>? = null,
    val expectedVersion: Long? = null,
    val includeJobExecutionState: Boolean? = null,
    val includeJobDocument: Boolean? = null,
    val executionNumber: Long? = null,
    val stepTimeoutInMinutes: Long? = null,
)

@JsonSerializable
data class UpdatedJobExecution(
    val executionState: JobExecutionState? = null,
    val jobDocument: String? = null,
)

@JsonSerializable
data class JobExecutionState(
    val status: JobExecutionStatus,
    val versionNumber: Long,
    val statusDetails: Map<String, String>? = null,
)
