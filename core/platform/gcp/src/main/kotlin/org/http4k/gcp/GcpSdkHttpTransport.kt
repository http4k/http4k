package org.http4k.gcp

import com.google.api.client.http.HttpTransport
import com.google.api.client.http.LowLevelHttpRequest
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

/**
 * Pluggable Http client adapter for GCP SDK.
 */
class GcpSdkHttpTransport(private val http: HttpHandler) : HttpTransport() {
    override fun buildRequest(method: String, uri: String): LowLevelHttpRequest =
        Http4kLowLevelHttpRequest(http, Request(Method.valueOf(method), uri))
}
