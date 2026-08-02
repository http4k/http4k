package org.http4k.connect.amazon.iotdataplane.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneAction
import org.http4k.connect.amazon.iotdataplane.IotDataPlaneMoshi
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.TimestampMillis
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class ListRetainedMessages(
    val maxResults: Int? = null,
    val nextToken: String? = null
) : IotDataPlaneAction<RetainedMessages> {

    override fun toRequest() = queryParameters()
        .fold(Request(GET, Uri.of("").path("/retainedMessage"))) { request, (name, value) -> request.query(name, value) }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(IotDataPlaneMoshi.asA<RetainedMessages>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun queryParameters() = listOfNotNull(
        maxResults?.let { "maxResults" to it.toString() },
        nextToken?.let { "nextToken" to it }
    )
}

/** AWS names this field `retainedTopics`, though it holds whole summaries rather than topic names. */
@JsonSerializable
data class RetainedMessages(
    val retainedTopics: List<RetainedMessageSummary> = emptyList(),
    val nextToken: String? = null
)

@JsonSerializable
data class RetainedMessageSummary(
    val topic: TopicName,
    val lastModifiedTime: TimestampMillis,
    val payloadSize: Long = 0,
    val qos: Int = 0
)
