package org.http4k.connect.gitlab.api

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface GitLab {
    operator fun <R> invoke(action: GitLabAction<R>): Result<R, RemoteFailure>

    companion object
}

