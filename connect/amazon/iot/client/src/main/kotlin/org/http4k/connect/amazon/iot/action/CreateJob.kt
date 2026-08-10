package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.TargetSelection
import org.http4k.connect.amazon.iot.model.TimeoutConfig
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
 * Creates a job which is sent to the [targets] (thing or thing group ARNs), carrying the
 * inline JSON [document]. A duplicate jobId is refused with ResourceAlreadyExistsException (409).
 *
 * The [document] is required: `documentSource` (an S3 link instead of an inline document) is
 * not supported, and neither are the rollout, retry, abort and scheduling configs.
 */
@Http4kConnectAction
data class CreateJob(
    val jobId: JobId,
    val targets: List<ARN>,
    val document: String,
    val description: String? = null,
    val targetSelection: TargetSelection? = null,
    val timeoutConfig: TimeoutConfig? = null,
) : IotAction<CreatedJob> {

    override fun toRequest() = Request(PUT, Uri.of("").path("/jobs/${jobId.value}"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(IotMoshi.asFormatString(CreateJobData(targets, document, description, targetSelection, timeoutConfig)))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<CreatedJob>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class CreateJobData(
    val targets: List<ARN> = emptyList(),
    val document: String? = null,
    val description: String? = null,
    val targetSelection: TargetSelection? = null,
    val timeoutConfig: TimeoutConfig? = null,
)

@JsonSerializable
data class CreatedJob(
    val jobArn: ARN,
    val jobId: JobId,
    val description: String? = null,
)
