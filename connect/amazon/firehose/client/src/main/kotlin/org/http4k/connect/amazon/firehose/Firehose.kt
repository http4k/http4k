package org.http4k.connect.amazon.firehose

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/firehose/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface Firehose {
    operator fun <R : Any> invoke(action: FirehoseAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("firehose")
}
