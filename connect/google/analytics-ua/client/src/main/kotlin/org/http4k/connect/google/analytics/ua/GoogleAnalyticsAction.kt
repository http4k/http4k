package org.http4k.connect.google.analytics.ua

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.RemoteFailure

@Http4kConnectAction
interface GoogleAnalyticsAction<R> : Action<Result<R, RemoteFailure>>
