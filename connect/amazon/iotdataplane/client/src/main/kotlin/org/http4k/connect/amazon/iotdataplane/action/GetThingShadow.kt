package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Response
import java.io.InputStream

/** The shadow document is opaque JSON, so it is returned as a stream, as S3 GetObject does. */
@Http4kConnectAction
data class GetThingShadow(
    val thingName: ThingName,
    val shadowName: ShadowName? = null
) : IotDataPlaneAction<InputStream> {

    override fun toRequest() = shadowRequest(GET, thingName, shadowName)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(body.stream)
            else -> Failure(asRemoteFailure(this))
        }
    }
}
