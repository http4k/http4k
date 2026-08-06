package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

/**
 * A job which is not in a terminal state (COMPLETED or CANCELED) can only be deleted with
 * [force] true; otherwise AWS answers with InvalidStateTransitionException (409).
 */
@Http4kConnectAction
data class DeleteJob(
    val jobId: JobId,
    val force: Boolean? = null,
) : IotAction<Unit> {

    override fun toRequest() = Request(DELETE, Uri.of("").path("/jobs/${jobId.value}"))
        .let { request -> force?.let { request.query("force", it.toString()) } ?: request }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
