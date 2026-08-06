package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.connect.amazon.iotjobsdataplane.action.JobExecutionState
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.GONE
import org.http4k.core.Status.Companion.NOT_FOUND

/** The AWS SDK turns these statuses and error headers into the correspondingly named exceptions. */
internal fun executionNotFound(jobId: String) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body("""{"message":"No execution of job $jobId exists for this thing"}""")

internal fun invalidRequest(message: String) = Response(BAD_REQUEST)
    .header("x-amzn-ErrorType", "InvalidRequestException")
    .body("""{"message":"$message"}""")

internal fun terminalState(jobId: String) = Response(GONE)
    .header("x-amzn-ErrorType", "TerminalStateException")
    .body("""{"message":"The execution of job $jobId is in a terminal state"}""")

/**
 * Per the AWS docs, the body of this error also contains the executionState field, which
 * saves the device a DescribeJobExecution round trip.
 */
internal fun invalidStateTransition(message: String, executionState: JobExecutionState) = Response(CONFLICT)
    .header("x-amzn-ErrorType", "InvalidStateTransitionException")
    .body("""{"message":"$message","executionState":${IotJobsDataPlaneMoshi.asFormatString(executionState)}}""")
