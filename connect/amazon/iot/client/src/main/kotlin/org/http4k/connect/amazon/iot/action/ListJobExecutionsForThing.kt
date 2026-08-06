package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.JobExecutionStatus
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

/** `namespaceId` (an AWS IoT Greengrass feature) is not supported. */
@Http4kConnectAction
data class ListJobExecutionsForThing(
    val thingName: ThingName,
    val status: JobExecutionStatus? = null,
    val jobId: JobId? = null,
    val maxResults: Int? = null,
    val nextToken: String? = null,
) : IotAction<JobExecutionsForThing> {

    override fun toRequest() = queryParameters()
        .fold(Request(GET, Uri.of("").path("/things/${thingName.value}/jobs"))) { request, (name, value) ->
            request.query(name, value)
        }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<JobExecutionsForThing>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun queryParameters() = listOfNotNull(
        jobId?.let { "jobId" to it.value },
        maxResults?.let { "maxResults" to it.toString() },
        nextToken?.let { "nextToken" to it },
        status?.let { "status" to it.name },
    )
}

@JsonSerializable
data class JobExecutionsForThing(
    val executionSummaries: List<JobExecutionSummaryForThing> = emptyList(),
    val nextToken: String? = null,
)

@JsonSerializable
data class JobExecutionSummaryForThing(
    val jobId: JobId,
    val jobExecutionSummary: JobExecutionSummary,
)

@JsonSerializable
data class JobExecutionSummary(
    val status: JobExecutionStatus? = null,
    val queuedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val executionNumber: Long? = null,
    val retryAttempt: Int? = null,
)
