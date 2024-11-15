package org.http4k.connect.google.analytics.ga4

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface GoogleAnalytics {
    operator fun <R> invoke(action: GoogleAnalyticsAction<R>): Result<R, RemoteFailure>

    companion object
}
