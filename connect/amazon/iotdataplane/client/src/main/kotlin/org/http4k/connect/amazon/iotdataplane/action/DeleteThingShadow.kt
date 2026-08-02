package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.DELETE
import org.http4k.core.Response
import java.io.InputStream

/** AWS answers with the deleted shadow's version as opaque JSON, so it is returned as a stream. */
@Http4kConnectAction
data class DeleteThingShadow(
    val thingName: ThingName,
    val shadowName: ShadowName? = null
) : IotDataPlaneAction<InputStream> {

    override fun toRequest() = shadowRequest(DELETE, thingName, shadowName)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(body.stream)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
