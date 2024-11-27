package org.http4k.connect.mattermost

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface Mattermost {
    operator fun <R> invoke(action: MattermostAction<R>): Result<R, RemoteFailure>

    companion object
}
