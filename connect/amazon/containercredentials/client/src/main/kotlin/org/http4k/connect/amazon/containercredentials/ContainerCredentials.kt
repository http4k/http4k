package org.http4k.connect.amazon.containercredentials

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

@Http4kConnectApiClient
interface ContainerCredentials {
    operator fun <R> invoke(action: ContainerCredentialsAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("containercredentials")
}
