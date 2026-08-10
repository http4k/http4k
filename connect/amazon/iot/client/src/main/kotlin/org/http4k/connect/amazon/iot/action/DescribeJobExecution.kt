package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
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

@Http4kConnectAction
data class DescribeJobExecution(
    val thingName: ThingName,
    val jobId: JobId,
    val executionNumber: Long? = null,
) : IotAction<DescribedJobExecution> {

    override fun toRequest() = Request(GET, Uri.of("").path("/things/${thingName.value}/jobs/${jobId.value}"))
        .let { request -> executionNumber?.let { request.query("executionNumber", it.toString()) } ?: request }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<DescribedJobExecution>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class DescribedJobExecution(
    val execution: JobExecution,
)

/**
 * Unlike the jobs data plane, the control plane wraps the status details map in a
 * `detailsMap` attribute.
 */
@JsonSerializable
data class JobExecution(
    val jobId: JobId? = null,
    val status: JobExecutionStatus? = null,
    val forceCanceled: Boolean? = null,
    val statusDetails: JobExecutionStatusDetails? = null,
    val thingArn: ARN? = null,
    val queuedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
    val executionNumber: Long? = null,
    val versionNumber: Long? = null,
    val approximateSecondsBeforeTimedOut: Long? = null,
)

@JsonSerializable
data class JobExecutionStatusDetails(
    val detailsMap: Map<String, String>? = null,
)
