package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class TrafficMetricsTest {

    private val baseTime = Instant.parse("2024-01-01T00:00:00Z")
    private var now = baseTime
    private val clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
    }

    private val registry = SimpleMeterRegistry()
    private val metrics = TrafficMetrics(registry, bucketSeconds = 10, maxBuckets = 6, clock = clock)

    private fun transaction(
        uri: String = "/test",
        status: org.http4k.core.Status = OK,
        duration: Duration = Duration.ofMillis(50),
        start: Instant = now
    ) = HttpTransaction(Request(GET, uri), Response(status), duration, start = start)

    @Test
    fun `empty timeline when no snapshots taken`() {
        val timeline = metrics.trafficTimeline()
        assertThat(timeline.labels.isEmpty(), equalTo(true))
        assertThat(timeline.datasets.values.all { it.isEmpty() }, equalTo(true))
    }

    @Test
    fun `records and snapshots produce correct per-interval counts`() {
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = NOT_FOUND), Inbound)
        metrics.snapshot()

        val timeline = metrics.trafficTimeline()
        assertThat(timeline.labels.size, equalTo(1))
        assertThat(timeline.datasets["2xx"], equalTo(listOf(2)))
        assertThat(timeline.datasets["4xx"], equalTo(listOf(1)))
        assertThat(timeline.datasets["3xx"], equalTo(listOf(0)))
        assertThat(timeline.datasets["5xx"], equalTo(listOf(0)))
    }

    @Test
    fun `multiple snapshots show deltas between intervals`() {
        metrics.record(transaction(status = OK), Inbound)
        metrics.snapshot()

        now = baseTime.plusSeconds(10)
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = INTERNAL_SERVER_ERROR), Inbound)
        metrics.snapshot()

        now = baseTime.plusSeconds(20)
        metrics.record(transaction(status = OK), Inbound)
        metrics.snapshot()

        val timeline = metrics.trafficTimeline()
        assertThat(timeline.labels.size, equalTo(2))
        assertThat(timeline.datasets["2xx"], equalTo(listOf(2, 1)))
        assertThat(timeline.datasets["5xx"], equalTo(listOf(1, 0)))
    }

    @Test
    fun `ring buffer evicts oldest entries when maxBuckets exceeded`() {
        repeat(8) { i ->
            now = baseTime.plusSeconds(i * 10L)
            metrics.record(transaction(status = OK), Inbound)
            metrics.snapshot()
        }

        val timeline = metrics.trafficTimeline()
        assertThat(timeline.labels.size, equalTo(6))
    }

    @Test
    fun `host timelines track outbound traffic per host`() {
        metrics.record(transaction(uri = "http://api.example.com/foo", status = OK), Outbound)
        metrics.record(transaction(uri = "http://api.example.com/bar", status = BAD_REQUEST), Outbound)
        metrics.record(transaction(uri = "http://other.com/baz", status = OK), Outbound)
        metrics.snapshot()

        val hostTimelines = metrics.hostTimelines()
        assertThat(hostTimelines.size, equalTo(2))

        val apiTimeline = hostTimelines["api.example.com"]!!
        assertThat(apiTimeline.datasets["2xx"], equalTo(listOf(1)))
        assertThat(apiTimeline.datasets["4xx"], equalTo(listOf(1)))

        val otherTimeline = hostTimelines["other.com"]!!
        assertThat(otherTimeline.datasets["2xx"], equalTo(listOf(1)))
    }

    @Test
    fun `host timelines include port when present`() {
        metrics.record(transaction(uri = "http://api.example.com:8080/foo", status = OK), Outbound)
        metrics.snapshot()

        val hostTimelines = metrics.hostTimelines()
        assertThat(hostTimelines.containsKey("api.example.com:8080"), equalTo(true))
    }

    @Test
    fun `inbound traffic is not tracked in host timelines`() {
        metrics.record(transaction(uri = "http://api.example.com/foo", status = OK), Inbound)
        metrics.snapshot()

        assertThat(metrics.hostTimelines().isEmpty(), equalTo(true))
    }

    @Test
    fun `labels show time ago from current clock`() {
        metrics.record(transaction(status = OK), Inbound)
        metrics.snapshot()

        now = baseTime.plusSeconds(10)
        metrics.record(transaction(status = OK), Inbound)
        metrics.snapshot()

        now = baseTime.plusSeconds(20)
        metrics.record(transaction(status = OK), Inbound)
        metrics.snapshot()

        now = baseTime.plusSeconds(30)
        val timeline = metrics.trafficTimeline()
        assertThat(timeline.labels, equalTo(listOf("20s", "10s")))
    }

    @Test
    fun `all status buckets tracked correctly`() {
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = SEE_OTHER), Inbound)
        metrics.record(transaction(status = BAD_REQUEST), Inbound)
        metrics.record(transaction(status = INTERNAL_SERVER_ERROR), Inbound)
        metrics.snapshot()

        val timeline = metrics.trafficTimeline()
        assertThat(timeline.datasets["2xx"], equalTo(listOf(1)))
        assertThat(timeline.datasets["3xx"], equalTo(listOf(1)))
        assertThat(timeline.datasets["4xx"], equalTo(listOf(1)))
        assertThat(timeline.datasets["5xx"], equalTo(listOf(1)))
    }

    @Test
    fun `total counts are available`() {
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = OK), Inbound)
        metrics.record(transaction(status = OK), Outbound)

        assertThat(metrics.totalRequests(), equalTo(3L))
        assertThat(metrics.inboundCount(), equalTo(2L))
        assertThat(metrics.outboundCount(), equalTo(1L))
    }
}
