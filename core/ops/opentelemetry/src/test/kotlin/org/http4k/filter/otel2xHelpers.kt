package org.http4k.filter

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.semconv.HttpAttributes
import org.http4k.core.Status

fun hasRequestDurationUnitOf(unit: String) =
    object : Matcher<List<MetricData>> {
        override val description = describe(unit)

        override fun invoke(actual: List<MetricData>): MatchResult {
            val summary = actual
                .firstOrNull { it.name == "http.server.request.duration" }
                ?: return Mismatch(describe(actual.toString()))
            return if (summary.unit == unit) Match else Mismatch(describe(actual.toString()))
        }
    }

fun hasRequestDuration(count: Int, value: Double, attributes: Attributes) =
    object : Matcher<List<MetricData>> {
        override val description = describe(attributes)

        override fun invoke(actual: List<MetricData>): MatchResult {
            val summary = actual
                .firstOrNull { it.name == "http.server.request.duration" }
                ?.histogramData
                ?.points
                ?.firstOrNull { it.attributes == attributes }
                ?: return Mismatch(describe(actual))
            return if (summary.count == count.toLong() && summary.sum - value < 0.0001) Match
            else Mismatch(describe(summary))
        }
    }

fun hasClientRequestDuration(count: Int, value: Double, attributes: Attributes) =
    object : Matcher<List<MetricData>> {
        override val description = describe(attributes)

        override fun invoke(actual: List<MetricData>): MatchResult {
            val summary = actual
                .firstOrNull { it.name == "http.client.request.duration" }
                ?.histogramData
                ?.points
                ?.firstOrNull { it.attributes == attributes }
                ?: return Mismatch(describe(actual))
            return if (summary.count == count.toLong() && summary.sum - value < 0.0001) Match
            else Mismatch(describe(summary))
        }
    }

fun hasNoRequestDurationWithStatus(status: Status) =
    object : Matcher<List<MetricData>> {
        override val description = describe(status)

        override fun invoke(actual: List<MetricData>): MatchResult {
            val summary = actual
                .firstOrNull { it.name == "http.server.request.duration" }
                ?.histogramData
                ?.points
                ?.filter { histogramPointData ->
                    histogramPointData.attributes.asMap().containsKey(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)
                }
                ?: return Match

            return if (summary.none { histogramPointData ->
                    histogramPointData.attributes.asMap()
                        .let { attributes ->
                            attributes.containsKey(HttpAttributes.HTTP_RESPONSE_STATUS_CODE) &&
                            attributes[HttpAttributes.HTTP_RESPONSE_STATUS_CODE] == status
                    }
                }) Match else Mismatch(describe(actual))
        }
    }
