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
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable

/**
 * Replaces the stream's files, role or description, and returns the new [UpdatedStream.streamVersion].
 * Omitted members are left as they are.
 */
@Http4kConnectAction
data class UpdateStream(
    val streamId: StreamId,
    val files: List<StreamFile>? = null,
    val roleArn: ARN? = null,
    val description: String? = null,
) : IotAction<UpdatedStream> {

    override fun toRequest() = Request(PUT, Uri.of("").path("/streams/${streamId.value}"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(IotMoshi.asFormatString(UpdateStreamData(files, roleArn, description)))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<UpdatedStream>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class UpdateStreamData(
    val files: List<StreamFile>? = null,
    val roleArn: ARN? = null,
    val description: String? = null,
)

@JsonSerializable
data class UpdatedStream(
    val streamId: StreamId,
    val streamArn: ARN,
    val streamVersion: Int,
    val description: String? = null,
)
