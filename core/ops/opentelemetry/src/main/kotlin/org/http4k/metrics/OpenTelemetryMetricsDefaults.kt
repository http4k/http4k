package org.http4k.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.semconv.ErrorAttributes
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.NetworkAttributes
import io.opentelemetry.semconv.ServerAttributes
import io.opentelemetry.semconv.UrlAttributes
import org.http4k.core.HttpTransaction


data class OpenTelemetryMetricsDefaults(
    val metricsDescription: Pair<String, String>,
    val bucketBoundaryAdvice: List<Double> = DEFAULT_BUCKET_BOUNDARY_ADVICE,
    val metricsLabeler: MetricsLabeler
) {
    companion object {
        val DEFAULT_BUCKET_BOUNDARY_ADVICE =
            listOf(0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0)
        private val urlTemplates = "\\{(.+):.+}".toRegex()

        private fun String.replaceRegexes() = replace(urlTemplates, ":$1")

        // See https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpserverrequestduration
        val server = OpenTelemetryMetricsDefaults(
            "http.server.request.duration" to "Duration of HTTP server requests.",
            DEFAULT_BUCKET_BOUNDARY_ADVICE
        ) { tx ->
            Attributes.builder()
                .putBaseHttpInformationFrom(tx)
                .apply {
                    if (tx.request.uri.scheme.isNotBlank())
                        put(UrlAttributes.URL_SCHEME, tx.request.uri.scheme.lowercase())
                    put(
                        stringKey("http.route"),
                        if (tx.routingGroup != "UNMAPPED") "/${tx.routingGroup.replaceRegexes()}" else tx.routingGroup
                    )
                }
                .build()
        }

        // https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpclientrequestduration
        val client = OpenTelemetryMetricsDefaults(
            "http.client.request.duration" to "Duration of HTTP client requests."
        ) { tx ->
            Attributes.builder()
                .putBaseHttpInformationFrom(tx)
                .apply {
                    put(ServerAttributes.SERVER_ADDRESS, tx.request.uri.host.lowercase())
                    put(
                        ServerAttributes.SERVER_PORT,
                        tx.request.uri.port?.toLong() ?: if (tx.request.uri.scheme == "https") 443 else 80
                    )
                }
                .build()
        }

        private fun AttributesBuilder.putBaseHttpInformationFrom(tx: HttpTransaction): AttributesBuilder =
            apply {
                put(stringKey("http.request.method"), tx.request.method.toString().uppercase())
                put(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, tx.response.status.code.toLong())
                if (!tx.response.status.successful)
                    put(ErrorAttributes.ERROR_TYPE, tx.response.status.description)
                put(NetworkAttributes.NETWORK_PROTOCOL_VERSION, tx.request.version.removePrefix("HTTP/"))
            }
    }
}

typealias MetricsLabeler = (HttpTransaction) -> Attributes
