package org.http4k.connect.amazon.cloudwatch

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.cloudwatch.model.MetricDatum
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.MetricUnit
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class FakeCloudWatchTest : CloudWatchContract, FakeAwsContract {
    private val metrics = Storage.InMemory<MutableList<MetricDatum>>()
    private val clock = object: Clock() {
        override fun instant() = Instant.EPOCH + Duration.ofDays(365)
        override fun getZone() = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId?) = this
    }

    override val http = FakeCloudWatch(metrics = metrics, clock = clock)

    @Test
    fun `injected clock used for metric timestamps`() {
        val namespace = Namespace.of("http4k-connect-test-namespace")
        val metricName = MetricName.of("http4k-connect-test-metric-name")
        val time = clock.instant()

        cloudWatch.putMetricData(
            Namespace = namespace,
            EntityMetricData = null,
            MetricData = listOf(
                MetricDatum(
                    MetricName = metricName,
                    Unit = MetricUnit.Count_per_Second,
                    Value = 1.0,
                    Timestamp = null, // will be set by cloudwatch
                    Values = listOf(0.5, 1.0),
                    StorageResolution = 60,
                ),
            ),
            StrictEntityValidation = null,
        )

        val metricStatistics = cloudWatch.getMetricStatistics(
            MetricName = metricName,
            Namespace = namespace,
            StartTime = time.minusSeconds(60),
            EndTime = time.plusSeconds(60),
            Period = 60,
            Unit = MetricUnit.Count_per_Second,
        ).successValue()
        assertThat(metricStatistics.Datapoints, hasSize(equalTo(1)))
        assertThat(metricStatistics.Datapoints.first().Timestamp, equalTo(time))
    }
}
