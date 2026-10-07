package org.http4k.filter

interface OpenTelemetryAttributesKeys {
    val method: String
    val clientUrl: String
    val serverUrl: String?
    val userAgent: String
    val httpRoute: String
    val clientAddress: String
    val statusCode: String
}

// Following the OpenTelemetry Semantic Conventions v1.38.0
// https://opentelemetry.io/docs/specs/semconv/http/http-spans/
object OpenTelemetrySemanticConventions : OpenTelemetryAttributesKeys {
    override val method = "http.request.method"
    override val clientUrl = "url.full"
    override val serverUrl = null
    override val userAgent = "user_agent.original"
    override val httpRoute = "http.route"
    override val clientAddress = "client.address"
    override val statusCode = "http.response.status_code"
}
