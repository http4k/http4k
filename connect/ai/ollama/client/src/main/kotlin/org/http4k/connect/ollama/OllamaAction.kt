package org.http4k.connect.ollama

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface OllamaAction<R> : Action<Result<R, RemoteFailure>>
