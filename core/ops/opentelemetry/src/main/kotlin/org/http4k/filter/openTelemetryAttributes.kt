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
    override val method: String = "http.request.method"
    override val clientUrl = "url.full"
    override val serverUrl = null
    override val userAgent = "user_agent.original"
    override val httpRoute: String = "http.route"
    override val clientAddress = "client.address"
    override val statusCode: String = "http.response.status_code"
}

@Deprecated("To be removed in favour of OTel Semantic Conventions", ReplaceWith("OpenTelemetrySemanticConventions"))
object LegacyHttp4kConventions : OpenTelemetryAttributesKeys {
    override val method = "http.method"
    override val clientUrl = "http.url"
    override val serverUrl = "http.url"
    override val userAgent = "http.user_agent"
    override val httpRoute = "http.route"
    override val clientAddress = "http.client_ip"
    override val statusCode = "http.status_code"
}
