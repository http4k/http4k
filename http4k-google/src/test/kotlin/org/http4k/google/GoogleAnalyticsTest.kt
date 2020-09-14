package org.http4k.google

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RoutedRequest
import org.junit.jupiter.api.Test

class GoogleAnalyticsTest {
    private val testHttpClient = CapturingHttpHandler()
    private val analytics = GoogleAnalytics(testHttpClient, "TEST-TRACING-ID", { "TEST-CLIENT-ID" }).then { Response(OK) }

    @Test
    fun `logs routed request as page view`() {
        val request = RoutedRequest(Request(GET, "/some/world"), UriTemplate.from("/some/{hello}"))
        val response = analytics(request)

        assertThat(response, hasStatus(OK))
        assertThat(testHttpClient.captured, equalTo(Request(POST, "/collect")
            .form("v", "1")
            .form("tid", "TEST-TRACING-ID")
            .form("cid", "TEST-CLIENT-ID")
            .form("dt", "some/{hello}")
            .form("dp", "some/{hello}")
            .form("dh", "localhost")
        ))
    }
}

class CapturingHttpHandler : HttpHandler {
    var captured: Request? = null

    override fun invoke(request: Request): Response {
        captured = request
        return Response(OK)
    }
}
