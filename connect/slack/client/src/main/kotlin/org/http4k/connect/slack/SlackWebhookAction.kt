package org.http4k.connect.slack

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

abstract class SlackWebhookAction<R: Any> : Action<Result<R, RemoteFailure>>
