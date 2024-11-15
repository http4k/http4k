package org.http4k.connect.google.analytics.ga4

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.CapturingHttpHandler
import org.http4k.connect.google.analytics.filter.CollectPageView
import org.http4k.connect.google.analytics.ga4.model.ApiSecret
import org.http4k.connect.google.analytics.ga4.model.EventData
import org.http4k.connect.google.analytics.ga4.model.EventsData
import org.http4k.connect.google.analytics.ga4.model.MeasurementId
import org.http4k.connect.google.analytics.model.ClientId
import org.http4k.connect.google.analytics.model.UserAgent
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.CONTINUE
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RoutedRequest
import org.junit.jupiter.api.Test

class CollectPageViewTest {
    val testHttpClient = CapturingHttpHandler()
    val measurementId = "TEST-MEASUREMENT-ID"
    val apiSecret = "SECRET"
    val clientId = ClientId.of("TEST-CLIENT-ID")
    val client = GoogleAnalytics.Http(MeasurementId.of(measurementId), ApiSecret.of(apiSecret), testHttpClient)
    val analytics = CollectPageView({ client.collect(it) }, { clientId }).then {
        when {
            it.uri.path.contains("fail") -> Response(BAD_REQUEST)
            it.uri.path.contains("informational") -> Response(CONTINUE)
            it.uri.path.contains("redirect") -> Response(SEE_OTHER)
            else -> Response(OK)
        }
    }

    @Test
    fun `logs request as page view`() {
        val response = analytics(Request(GET, "https://www.http4k.org/some/world"))

        assertThat(response, hasStatus(OK))
        assertPageView("/some/world", "/some/world", "www.http4k.org")
    }

    @Test
    fun `logs routed request as page view`() {
        val response = analytics(RoutedRequest(Request(GET, "/some/world"), UriTemplate.from("/some/{hello}")))

        assertThat(response, hasStatus(OK))
        assertPageView("some/{hello}", "some/{hello}", "")
    }

    @Test
    fun `logs request with host as page view`() {
        val response = analytics(Request(GET, "/some/world").header("host", "www.http4k.org"))

        assertThat(response, hasStatus(OK))
        assertPageView("/some/world", "/some/world", "www.http4k.org")
    }

    @Test
    fun `logs page view for informational response`() {
        val response = analytics(Request(GET, "/informational"))

        assertThat(response, hasStatus(CONTINUE))
        assertPageView("/informational", "/informational", "")
    }

    @Test
    fun `logs page view for redirect response`() {
        val response = analytics(Request(GET, "/redirect"))

        assertThat(response, hasStatus(SEE_OTHER))
        assertPageView("/redirect", "/redirect", "")
    }

    @Test
    fun `ignore bad responses from google analytics`() {
        testHttpClient.response = Response(CLIENT_TIMEOUT)

        val response = analytics(Request(GET, "some/world"))

        assertThat(response, hasStatus(OK))
        assertPageView("some/world", "some/world", "")
    }

    @Test
    fun `don't log page view on unsuccessful response`() {
        val response = analytics(Request(GET, "/fail"))

        assertThat(response, hasStatus(BAD_REQUEST))
        assertNoPageView()
    }

    fun assertPageView(title: String, path: String, host: String) {
        assertThat(
            testHttpClient.captured, equalTo(
                Request(Method.POST, "https://www.google-analytics.com/mp/collect")
                    .header("User-Agent", UserAgent.Default.value)
                    .header("content-type", APPLICATION_JSON.toHeaderValue())
                    .header("Host", "www.google-analytics.com")
                    .query("measurement_id", measurementId)
                    .query("api_secret", apiSecret)
                    .body(
                        GoogleAnalyticsMoshi.asFormatString(
                            EventsData(
                                client_id = clientId,
                                events = listOf(
                                    EventData(
                                        name = "page_view",
                                        params = mapOf(
                                            "page_location" to Uri.of("https://$host/$path"),
                                            "page_title" to title
                                        )
                                    )
                                )
                            )
                        )
                    )
            )
        )
    }

    fun assertNoPageView() {
        assertThat(testHttpClient.captured, absent())
    }

}

