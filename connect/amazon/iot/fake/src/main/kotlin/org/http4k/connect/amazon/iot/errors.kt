package org.http4k.connect.amazon.iot

import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CONFLICT
import org.http4k.core.Status.Companion.NOT_FOUND

/** The AWS SDK turns these statuses and error headers into the correspondingly named exceptions. */
internal fun jobNotFound(jobId: JobId) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body("""{"message":"Job ${jobId.value} does not exist"}""")

internal fun executionNotFound(jobId: JobId) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body("""{"message":"No execution of job ${jobId.value} exists for this thing"}""")

internal fun jobAlreadyExists(job: StoredJob) = Response(CONFLICT)
    .header("x-amzn-ErrorType", "ResourceAlreadyExistsException")
    .body(
        """{"message":"Job ${job.jobId.value} already exists",""" +
            """"resourceId":"${job.jobId.value}","resourceArn":"${job.jobArn.value}"}"""
    )

internal fun streamNotFound(streamId: StreamId) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body("""{"message":"Stream ${streamId.value} does not exist"}""")

internal fun streamAlreadyExists(stream: StoredStream) = Response(CONFLICT)
    .header("x-amzn-ErrorType", "ResourceAlreadyExistsException")
    .body(
        """{"message":"Stream ${stream.streamId.value} already exists",""" +
            """"resourceId":"${stream.streamId.value}","resourceArn":"${stream.streamArn.value}"}"""
    )

internal fun invalidRequest(message: String) = Response(BAD_REQUEST)
    .header("x-amzn-ErrorType", "InvalidRequestException")
    .body("""{"message":"$message"}""")

internal fun invalidStateTransition(message: String) = Response(CONFLICT)
    .header("x-amzn-ErrorType", "InvalidStateTransitionException")
    .body("""{"message":"$message"}""")
