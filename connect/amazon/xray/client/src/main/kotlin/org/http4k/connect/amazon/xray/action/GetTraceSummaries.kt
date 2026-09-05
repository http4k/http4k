package org.http4k.connect.amazon.xray.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.xray.XRayAction
import org.http4k.connect.amazon.xray.XRayMoshi
import org.http4k.connect.amazon.xray.model.TimeRangeType
import org.http4k.connect.amazon.xray.model.TraceSummary
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetTraceSummaries(
    val StartTime: Timestamp,
    val EndTime: Timestamp,
    val FilterExpression: String? = null,
    val TimeRangeType: TimeRangeType? = null,
    val Sampling: Boolean? = null,
    val NextToken: String? = null,
) : XRayAction<TraceSummaries> {

    override fun toRequest() = Request(POST, Uri.of("").path("/TraceSummaries"))
        .with(CONTENT_TYPE of APPLICATION_JSON)
        .body(
            XRayMoshi.asFormatString(
                GetTraceSummariesData(
                    StartTime.value, EndTime.value, FilterExpression, TimeRangeType, Sampling, NextToken
                )
            )
        )

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(XRayMoshi.asA<TraceSummaries>(bodyString()))
            else -> Failure(asRemoteFailure(this))
        }
    }
}

/** The times go on the wire as whole epoch seconds, which is what every AWS SDK sends. */
@JsonSerializable
data class GetTraceSummariesData(
    val StartTime: Long,
    val EndTime: Long,
    val FilterExpression: String? = null,
    val TimeRangeType: TimeRangeType? = null,
    val Sampling: Boolean? = null,
    val NextToken: String? = null,
)

@JsonSerializable
data class TraceSummaries(
    val TraceSummaries: List<TraceSummary> = emptyList(),
    val ApproximateTime: Timestamp? = null,
    val TracesProcessedCount: Long? = null,
    val NextToken: String? = null,
)
