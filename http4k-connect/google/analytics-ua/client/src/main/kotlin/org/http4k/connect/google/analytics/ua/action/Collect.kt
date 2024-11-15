package org.http4k.connect.google.analytics.ua.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.google.analytics.model.Analytics
import org.http4k.connect.google.analytics.model.Event
import org.http4k.connect.google.analytics.model.PageView
import org.http4k.connect.google.analytics.ua.GoogleAnalyticsAction
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.body.form

@Http4kConnectAction
data class Collect(val data: Analytics) : GoogleAnalyticsAction<Unit> {
    override fun toRequest() = with(data) {
        val request = Request(POST, uri())
            .header("User-Agent", userAgent.value)
            .form(CLIENT_ID, clientId.value)

        when (this) {
            is Event -> request
                .form(EVENT_TYPE, "event")
                .form(EVENT_CATEGORY, category)
                .form(EVENT_ACTION, action)
                .form(EVENT_LABEL, label)
                .run { value?.let { form(EVENT_VALUE, it.toString()) } ?: this }

            is PageView -> request
                .form(DOCUMENT_TITLE, title)
                .form(DOCUMENT_PATH, path)
                .form(DOCUMENT_HOST, host)
        }
    }

    override fun toResult(response: Response) = with(response) {
        if (status.successful) Success(Unit) else Failure(asRemoteFailure(this))
    }

    private fun uri() = Uri.of("/collect")
}

const val CLIENT_ID = "cid"

const val DOCUMENT_TITLE = "dt"
const val DOCUMENT_PATH = "dp"
const val DOCUMENT_HOST = "dh"

const val EVENT_TYPE = "t"
const val EVENT_ACTION = "ea"
const val EVENT_CATEGORY = "ec"
const val EVENT_LABEL = "el"
const val EVENT_VALUE = "ev"
