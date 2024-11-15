package org.http4k.connect.amazon.evidently

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

@Http4kConnectApiClient
interface Evidently {
    operator fun <R : Any> invoke(action: EvidentlyAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("evidently")
}
