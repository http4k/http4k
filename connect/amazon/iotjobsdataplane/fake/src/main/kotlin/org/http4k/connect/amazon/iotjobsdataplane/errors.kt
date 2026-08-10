package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.connect.amazon.iotjobsdataplane.action.JobExecutionState
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.GONE
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.format.unwrap

/**
 * The AWS SDK turns these statuses and error headers into the correspondingly named exceptions.
 *
 * The body is marshalled rather than interpolated, because some of these messages carry values
 * straight off the request, which would otherwise be able to break out of the JSON.
 */
private fun error(status: Status, type: String, body: Map<String, Any?>) = Response(status)
    .header("x-amzn-ErrorType", type)
    .body(IotJobsDataPlaneMoshi.asFormatString(body))

internal fun executionNotFound(jobId: String) = error(
    NOT_FOUND, "ResourceNotFoundException",
    mapOf("message" to "No execution of job $jobId exists for this thing")
)

internal fun invalidRequest(message: String) =
    error(BAD_REQUEST, "InvalidRequestException", mapOf("message" to message))

internal fun terminalState(jobId: String) = error(
    GONE, "TerminalStateException",
    mapOf("message" to "The execution of job $jobId is in a terminal state")
)

/**
 * Per the AWS docs, the body of this error also contains the executionState field, which
 * saves the device a DescribeJobExecution round trip.
 */
internal fun invalidStateTransition(message: String, executionState: JobExecutionState) = error(
    CONFLICT, "InvalidStateTransitionException",
    mapOf(
        "message" to message,
        // to a plain map, so that the state nests as an object instead of being stringified
        "executionState" to IotJobsDataPlaneMoshi.asJsonObject(executionState).unwrap(),
    )
)
