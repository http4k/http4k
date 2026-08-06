package org.http4k.connect.amazon.iotjobsdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneAction
import org.http4k.connect.amazon.iotjobsdataplane.IotJobsDataPlaneMoshi
import org.http4k.connect.amazon.iotjobsdataplane.model.JobExecution
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

/**
 * Gets details of a job execution, without any state transition: [jobId] may be [JobId.NEXT]
 * (the literal `$next`), which reads the device's next pending execution - the oldest
 * IN_PROGRESS one, else the oldest QUEUED one - and, being read-only, is safe to poll on
 * every connect. With `$next` and nothing pending the service answers 200 with an empty
 * document, surfaced here as a null [DescribedJobExecution.execution] (the AWS docs imply
 * this by leaving the response's execution member optional).
 *
 * [includeJobDocument] defaults to true ON THE SERVER when the query is omitted, per the
 * AWS API reference ("Unless set to false, the response contains the job document. The
 * default is true.").
 */
@Http4kConnectAction
data class DescribeJobExecution(
    val thingName: ThingName,
    val jobId: JobId,
    val executionNumber: Long? = null,
    val includeJobDocument: Boolean? = null,
) : IotJobsDataPlaneAction<DescribedJobExecution> {

    override fun toRequest() = queryParameters()
        .fold(Request(GET, Uri.of("").path("/things/${thingName.value}/jobs/${jobId.value}"))) { request, (name, value) ->
            request.query(name, value)
        }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotJobsDataPlaneMoshi.asA<DescribedJobExecution>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun queryParameters() = listOfNotNull(
        executionNumber?.let { "executionNumber" to it.toString() },
        includeJobDocument?.let { "includeJobDocument" to it.toString() },
    )
}

@JsonSerializable
data class DescribedJobExecution(
    val execution: JobExecution? = null,
)
