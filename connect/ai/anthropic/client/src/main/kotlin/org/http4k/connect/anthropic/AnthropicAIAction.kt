package org.http4k.connect.anthropic

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface AnthropicAIAction<R> : Action<Result<R, RemoteFailure>>
