package org.http4k.connect.azure

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface AzureAIAction<R> : Action<Result<R, RemoteFailure>>
