package org.http4k.connect.amazon.iamidentitycenter

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

@Http4kConnectApiClient
interface SSO {
    operator fun <R : Any> invoke(action: SSOAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("sso")
}
