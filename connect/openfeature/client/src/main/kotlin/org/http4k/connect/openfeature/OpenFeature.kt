package org.http4k.connect.openfeature

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure

@Http4kConnectApiClient
interface OpenFeature {
    operator fun <R : Any> invoke(action: OpenFeatureAction<R>): Result<R, RemoteFailure>

    companion object
}
