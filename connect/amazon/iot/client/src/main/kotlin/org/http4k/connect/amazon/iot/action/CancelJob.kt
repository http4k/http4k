package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.JobId
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
 * With [force] false (the default) only QUEUED executions are canceled; IN_PROGRESS
 * executions are canceled only when [force] is true.
 */
@Http4kConnectAction
data class CancelJob(
    val jobId: JobId,
    val comment: String? = null,
    val force: Boolean? = null,
    val reasonCode: String? = null,
) : IotAction<CancelledJob> {

    override fun toRequest() = Request(PUT, Uri.of("").path("/jobs/${jobId.value}/cancel"))
        .let { request -> force?.let { request.query("force", it.toString()) } ?: request }
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(IotMoshi.asFormatString(CancelJobData(comment, reasonCode)))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<CancelledJob>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class CancelJobData(
    val comment: String? = null,
    val reasonCode: String? = null,
)

@JsonSerializable
data class CancelledJob(
    val jobArn: ARN,
    val jobId: JobId,
    val description: String? = null,
)
