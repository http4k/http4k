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
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class DescribeStream(
    val streamId: StreamId,
) : IotAction<DescribedStream> {

    override fun toRequest() = Request(GET, Uri.of("").path("/streams/${streamId.value}"))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<DescribedStream>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class DescribedStream(
    val streamInfo: StreamInfo,
)

@JsonSerializable
data class StreamInfo(
    val streamId: StreamId,
    val streamArn: ARN,
    val streamVersion: Int,
    val files: List<StreamFile> = emptyList(),
    val roleArn: ARN? = null,
    val description: String? = null,
    val createdAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null,
)
