package org.http4k.connect.google.analytics.ga4.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.google.analytics.ga4.GoogleAnalyticsAction
import org.http4k.connect.google.analytics.ga4.model.EventData
import org.http4k.connect.google.analytics.ga4.model.EventsData
import org.http4k.connect.google.analytics.model.Analytics
import org.http4k.connect.google.analytics.model.Event
import org.http4k.connect.google.analytics.model.PageView
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE

@Http4kConnectAction(docs = "https://developers.google.com/analytics/devguides/collection/protocol/ga4/reference?client_type=gtag https://support.google.com/analytics/answer/9216061")
data class Collect(val data: Analytics) : GoogleAnalyticsAction<Unit>() {
    override fun toRequest() = with(data) {
        val request = Request(POST, uri())
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .header("User-Agent", userAgent.value)

        when (this) {
            is Event -> request
                .body(
                    autoMarshalling.asFormatString(
                        EventsData(
                            client_id = clientId,
                            events = listOf(EventData(
                                name = category,
                                params = mapOf(
                                    "action" to action,
                                    "label" to label
                                ) + (value?.let { mapOf("value" to it.toString()) } ?: emptyMap())
                            ))
                        )
                    )
                )

            is PageView -> request
                .body(
                    autoMarshalling.asFormatString(
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
        }
    }

    override fun toResult(response: Response) = with(response) {
        if (status.successful) Success(Unit) else Failure(asRemoteFailure(this))
    }

    private fun uri() = Uri.of("/mp/collect")
}
