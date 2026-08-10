package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.StreamFile
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable

/**
 * Creates a stream of [files] (1 to 50) which devices pull over MQTT file delivery. [roleArn]
 * is the role IoT assumes to read the S3 objects, and is required. A duplicate streamId is
 * refused with ResourceAlreadyExistsException (409).
 *
 * Devices read the blocks over MQTT (`$aws/things/{thing}/streams/{streamId}/get/cbor`), not
 * over HTTP, so there is no data-plane client here - only this control plane, which publishes
 * the bytes for a device to fetch.
 *
 * `tags` is not supported.
 */
@Http4kConnectAction
data class CreateStream(
    val streamId: StreamId,
    val files: List<StreamFile>,
    val roleArn: ARN,
    val description: String? = null,
) : IotAction<CreatedStream> {

    override fun toRequest() = Request(POST, Uri.of("").path("/streams/${streamId.value}"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(IotMoshi.asFormatString(CreateStreamData(files, roleArn, description)))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<CreatedStream>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class CreateStreamData(
    val files: List<StreamFile> = emptyList(),
    val roleArn: ARN? = null,
    val description: String? = null,
)

@JsonSerializable
data class CreatedStream(
    val streamId: StreamId,
    val streamArn: ARN,
    val streamVersion: Int,
    val description: String? = null,
)
