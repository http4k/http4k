package org.http4k.connect.amazon.xray

import org.http4k.connect.amazon.xray.model.Segment
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.connect.model.Timestamp

data class StoredTrace(
    val traceId: TraceId,
    val startTime: Timestamp,
    val duration: Double? = null,
    val annotations: Map<String, String> = emptyMap(),
    val segments: List<Segment> = emptyList(),
)
