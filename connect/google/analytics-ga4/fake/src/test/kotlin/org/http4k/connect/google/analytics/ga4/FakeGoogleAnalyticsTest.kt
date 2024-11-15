package org.http4k.connect.google.analytics.ga4

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.google.analytics.ga4.model.ApiSecret
import org.http4k.connect.google.analytics.ga4.model.MeasurementId
import org.http4k.connect.google.analytics.model.ClientId
import org.http4k.connect.google.analytics.model.Event
import org.http4k.connect.google.analytics.model.PageView
import org.http4k.connect.google.analytics.model.UserAgent
import org.junit.jupiter.api.Test

class FakeGoogleAnalyticsTest {
    private val analytics =
        GoogleAnalytics.Http(MeasurementId.of("SOME_TRACKING_ID"), ApiSecret.of("SECRET"), FakeGoogleAnalytics())

    @Test
    fun `can log page view`() {
        assertThat(
            analytics.collect(
                PageView(
                    "title",
                    "/doc/path",
                    "www.http4k.org",
                    ClientId.of("SOME_CLIENT_ID"),
                    UserAgent.of("some-user-agent")
                )
            ),
            equalTo(Success(Unit))
        )
    }

    @Test
    fun `can log event`() {
        assertThat(
            analytics.collect(
                Event(
                    "event",
                    "action",
                    "label",
                    1,
                    ClientId.of("SOME_CLIENT_ID"),
                    UserAgent.of("some-user-agent")
                )
            ),
            equalTo(Success(Unit))
        )
    }
}

