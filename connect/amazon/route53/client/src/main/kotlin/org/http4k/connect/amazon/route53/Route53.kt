package org.http4k.connect.amazon.route53

import dev.forkhandles.result4k.Result
import org.http4k.connect.Action
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.route53.action.Route53Action

@Http4kConnectApiClient
interface Route53 {
    operator fun <R> invoke(action: Route53Action<R>): Result<R, RemoteFailure>

    companion object
}

