package org.http4k.connect.google.analytics.ga4

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure
import org.http4k.format.AutoMarshalling

@Http4kConnectAction
abstract class GoogleAnalyticsAction<R>(val autoMarshalling: AutoMarshalling = GoogleAnalyticsMoshi) :
    Action<Result<R, RemoteFailure>>
