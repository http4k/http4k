package org.http4k.connect.mattermost

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure

interface MattermostAction<R> : Action<Result<R, RemoteFailure>>
