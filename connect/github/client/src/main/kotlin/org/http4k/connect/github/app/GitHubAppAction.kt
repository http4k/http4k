package org.http4k.connect.github.app

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface GitHubAppAction<R> : Action<Result<R, RemoteFailure>>
