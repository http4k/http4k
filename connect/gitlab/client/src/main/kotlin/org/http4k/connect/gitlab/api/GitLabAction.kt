package org.http4k.connect.gitlab.api

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface GitLabAction<R> : Action<Result<R, RemoteFailure>>
