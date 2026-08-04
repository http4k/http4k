package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.model.ClientId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
data class DeleteConnection(
    val clientId: ClientId,
    val cleanSession: Boolean? = null,
    val preventWillMessage: Boolean? = null
) : IotDataPlaneAction<Unit> {

    override fun toRequest() = queryParameters()
        .fold(Request(DELETE, uri())) { request, (name, value) -> request.query(name, value) }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("").path("/connections/${clientId.value}")

    private fun queryParameters() = listOfNotNull(
        cleanSession?.let { "cleanSession" to it.toString() },
        preventWillMessage?.let { "preventWillMessage" to it.toString() }
    )
}
