package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.MemoryBody
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import java.io.InputStream

/** Both documents are opaque JSON, so they are carried as raw bytes in and a stream out. */
@Http4kConnectAction
data class UpdateThingShadow(
    val thingName: ThingName,
    val payload: ByteArray,
    val shadowName: ShadowName? = null
) : IotDataPlaneAction<InputStream> {

    override fun toRequest() = shadowRequest(POST, thingName, shadowName)
        .with(CONTENT_TYPE of OCTET_STREAM)
        .body(MemoryBody(payload))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(body.stream)
            else -> Failure(asRemoteFailure(this))
        }
    }

    /** ByteArray equality is by identity, so equals/hashCode have to compare the payload here. */
    private fun fieldsOtherThanPayload() = listOf(thingName, shadowName)

    override fun equals(other: Any?) = other is UpdateThingShadow &&
        payload.contentEquals(other.payload) &&
        fieldsOtherThanPayload() == other.fieldsOtherThanPayload()

    override fun hashCode() = 31 * fieldsOtherThanPayload().hashCode() + payload.contentHashCode()
}
