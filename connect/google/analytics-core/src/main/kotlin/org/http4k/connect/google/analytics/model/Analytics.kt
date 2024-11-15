package org.http4k.connect.google.analytics.model

import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure
import org.http4k.core.Uri

sealed class Analytics {
    abstract val clientId: ClientId
    abstract val userAgent: UserAgent
}

data class Event(
    val category: String,
    val action: String = "",
    val label: String = "",
    val value: Int? = null,
    override val clientId: ClientId,
    override val userAgent: UserAgent = UserAgent.Default
) : Analytics()

data class PageView(
    val title: String,
    val path: String,
    val host: String,
    override val clientId: ClientId,
    override val userAgent: UserAgent = UserAgent.Default
) : Analytics()

typealias AnalyticsCollector = (Analytics) -> Result<Unit, RemoteFailure>

val GOOGLE_ANALYTICS_URL = Uri.of("https://www.google-analytics.com")
