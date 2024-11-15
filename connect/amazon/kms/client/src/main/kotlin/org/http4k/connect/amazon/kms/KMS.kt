package org.http4k.connect.amazon.kms

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/kms/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface KMS {
    operator fun <R : Any> invoke(action: KMSAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("kms")
}
