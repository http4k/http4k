package org.http4k.connect.amazon.iot.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.IotAction
import org.http4k.connect.amazon.iot.IotMoshi
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

/** [ascendingOrder] rides as `isAscendingOrder`, which is the name AWS gives the query parameter. */
@Http4kConnectAction
data class ListStreams(
    val maxResults: Int? = null,
    val nextToken: String? = null,
    val ascendingOrder: Boolean? = null,
) : IotAction<Streams> {

    override fun toRequest() = Request(GET, Uri.of("").path("/streams"))
        .let { request -> maxResults?.let { request.query("maxResults", it.toString()) } ?: request }
        .let { request -> nextToken?.let { request.query("nextToken", it) } ?: request }
        .let { request -> ascendingOrder?.let { request.query("isAscendingOrder", it.toString()) } ?: request }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotMoshi.asA<Streams>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class Streams(
    val streams: List<StreamSummary> = emptyList(),
    val nextToken: String? = null,
)

@JsonSerializable
data class StreamSummary(
    val streamId: StreamId,
    val streamArn: ARN,
    val streamVersion: Int,
    val description: String? = null,
)
