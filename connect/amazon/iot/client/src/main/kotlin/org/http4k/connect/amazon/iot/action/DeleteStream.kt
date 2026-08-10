package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

/**
 * Deletes the stream. A stream a job still refers to is refused with DeleteConflictException
 * (409); unlike DeleteJob there is no force parameter.
 */
@Http4kConnectAction
data class DeleteStream(
    val streamId: StreamId,
) : IotAction<Unit> {

    override fun toRequest() = Request(DELETE, Uri.of("").path("/streams/${streamId.value}"))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
