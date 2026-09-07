package org.http4k.connect.amazon.xray.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Trace(
    val Id: TraceId,
    val Duration: Double? = null,
    val LimitExceeded: Boolean? = null,
    val Segments: List<Segment> = emptyList(),
)

@JsonSerializable
data class Segment(
    val Id: SegmentId,
    val Document: String? = null,
)
