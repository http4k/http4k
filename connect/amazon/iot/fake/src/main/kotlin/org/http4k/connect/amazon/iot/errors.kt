package org.http4k.connect.amazon.iot

import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND

/**
 * The AWS SDK turns these statuses and error headers into the correspondingly named exceptions.
 *
 * The body is marshalled rather than interpolated, because some of these messages carry values
 * straight off the request, which would otherwise be able to break out of the JSON.
 */
private fun error(status: Status, type: String, body: Map<String, String>) = Response(status)
    .header("x-amzn-ErrorType", type)
    .body(IotMoshi.asFormatString(body))

internal fun jobNotFound(jobId: JobId) =
    error(NOT_FOUND, "ResourceNotFoundException", mapOf("message" to "Job ${jobId.value} does not exist"))

internal fun executionNotFound(jobId: JobId) = error(
    NOT_FOUND, "ResourceNotFoundException",
    mapOf("message" to "No execution of job ${jobId.value} exists for this thing")
)

internal fun jobAlreadyExists(job: StoredJob) = error(
    CONFLICT, "ResourceAlreadyExistsException",
    mapOf(
        "message" to "Job ${job.jobId.value} already exists",
        "resourceId" to job.jobId.value,
        "resourceArn" to job.jobArn.value,
    )
)

internal fun streamNotFound(streamId: StreamId) =
    error(NOT_FOUND, "ResourceNotFoundException", mapOf("message" to "Stream ${streamId.value} does not exist"))

internal fun streamAlreadyExists(stream: StoredStream) = error(
    CONFLICT, "ResourceAlreadyExistsException",
    mapOf(
        "message" to "Stream ${stream.streamId.value} already exists",
        "resourceId" to stream.streamId.value,
        "resourceArn" to stream.streamArn.value,
    )
)

internal fun invalidRequest(message: String) =
    error(BAD_REQUEST, "InvalidRequestException", mapOf("message" to message))

internal fun invalidStateTransition(message: String) =
    error(CONFLICT, "InvalidStateTransitionException", mapOf("message" to message))
