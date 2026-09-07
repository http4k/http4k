package org.http4k.connect.amazon.xray.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.xray.XRayAction
import org.http4k.connect.amazon.xray.XRayMoshi
import org.http4k.connect.amazon.xray.model.Trace
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable

/** AWS takes at most 5 trace ids per call. */
@Http4kConnectAction
data class BatchGetTraces(
    val TraceIds: List<TraceId>,
    val NextToken: String? = null,
) : XRayAction<Traces> {

    override fun toRequest() = Request(POST, Uri.of("").path("/Traces"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(XRayMoshi.asFormatString(BatchGetTracesData(TraceIds, NextToken)))

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(XRayMoshi.asA<Traces>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

@JsonSerializable
data class BatchGetTracesData(
    val TraceIds: List<TraceId> = emptyList(),
    val NextToken: String? = null,
)

@JsonSerializable
data class Traces(
    val Traces: List<Trace> = emptyList(),
    val UnprocessedTraceIds: List<TraceId> = emptyList(),
    val NextToken: String? = null,
)
