package org.http4k.connect.amazon.xray

import org.http4k.aws.AwsCredentials
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.xray.action.BatchGetTraces
import org.http4k.connect.amazon.xray.action.GetTraceSummaries
import org.http4k.connect.amazon.xray.action.TraceSummaries
import org.http4k.connect.amazon.xray.action.Traces
import org.http4k.connect.amazon.xray.model.AnnotationValue
import org.http4k.connect.amazon.xray.model.Trace
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.connect.amazon.xray.model.TraceSummary
import org.http4k.connect.amazon.xray.model.ValueWithServiceIds
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock

/**
 * The [FILTER] subset is the only filter expression evaluated: anything else is refused rather
 * than answered with every trace in the window.
 */
class FakeXRay(
    private val traces: Storage<StoredTrace> = Storage.InMemory(),
    private val region: Region = Region.of("ldn-north-1"),
    private val clock: Clock = Clock.systemUTC(),
) : ChaoticHttpHandler() {

    override val app = routes(
        "/TraceSummaries" bind POST to ::getTraceSummaries,
        "/Traces" bind POST to ::batchGetTraces,
    )

    private fun getTraceSummaries(request: Request): Response {
        val query = XRayMoshi.asA<GetTraceSummaries>(request.bodyString().ifEmpty { "{}" })

        val annotation = query.FilterExpression?.let {
            FILTER.matchEntire(it.trim())?.destructured
                ?: return invalidRequest("Invalid filter expression: ${query.FilterExpression}")
        }

        val matching = traces.keySet()
            .mapNotNull { traces[it] }
            .filter { it.startTime.value >= query.StartTime.value && it.startTime.value < query.EndTime.value }
            .filter { trace -> annotation?.let { (key, value) -> trace.annotations[key] == value } ?: true }
            .sortedBy { it.startTime.value }

        val from = request.query("nextToken")?.toIntOrNull() ?: query.NextToken?.toIntOrNull() ?: 0
        val page = matching.drop(from).take(PAGE_SIZE)

        return Response(OK).body(
            XRayMoshi.asFormatString(
                TraceSummaries(
                    TraceSummaries = page.map { it.toSummary() },
                    ApproximateTime = Timestamp.of(clock.instant()),
                    TracesProcessedCount = matching.size.toLong(),
                    NextToken = (from + page.size).takeIf { it < matching.size }?.toString(),
                )
            )
        )
    }

    private fun batchGetTraces(request: Request): Response {
        val query = XRayMoshi.asA<BatchGetTraces>(request.bodyString().ifEmpty { """{"TraceIds":[]}""" })

        if (query.TraceIds.size > MAX_TRACE_IDS) {
            return invalidRequest("A maximum of $MAX_TRACE_IDS trace ids may be requested at a time")
        }

        val found = query.TraceIds.mapNotNull { traces[it.value] }

        return Response(OK).body(
            XRayMoshi.asFormatString(
                Traces(
                    Traces = found.map { Trace(it.traceId, it.duration, Segments = it.segments) },
                    UnprocessedTraceIds = query.TraceIds.filterNot { traces[it.value] != null },
                )
            )
        )
    }

    private fun StoredTrace.toSummary() = TraceSummary(
        Id = traceId,
        StartTime = startTime,
        Duration = duration,
        Annotations = annotations
            .takeIf { it.isNotEmpty() }
            ?.mapValues { (_, value) -> listOf(ValueWithServiceIds(AnnotationValue(StringValue = value))) },
    )

    fun client() = XRay.Http(region, { AwsCredentials("accessKey", "secret") }, this)

    fun trace(traceId: TraceId) = traces[traceId.value]

    companion object {
        private val FILTER = """annotation\.([A-Za-z0-9_.]+)\s*=\s*"([^"]*)"""".toRegex()
        private const val MAX_TRACE_IDS = 5
        private const val PAGE_SIZE = 100
    }
}

fun main() {
    FakeXRay().start()
}
