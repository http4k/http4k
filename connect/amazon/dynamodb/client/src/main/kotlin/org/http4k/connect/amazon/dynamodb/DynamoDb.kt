package org.http4k.connect.amazon.dynamodb

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Operations.html
 */
@Http4kConnectApiClient
interface DynamoDb {
    operator fun <R : Any> invoke(action: DynamoDbAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("dynamodb")
}
