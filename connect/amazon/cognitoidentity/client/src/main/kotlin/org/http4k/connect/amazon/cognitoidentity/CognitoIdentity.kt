package org.http4k.connect.amazon.cognitoidentity

import dev.forkhandles.result4k.Result
import org.http4k.connect.Http4kConnectApiClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AwsServiceCompanion

/**
 * Docs: https://docs.aws.amazon.com/cognitoidentity/latest/APIReference/Welcome.html
 */
@Http4kConnectApiClient
interface CognitoIdentity {
    operator fun <R : Any> invoke(action: CognitoIdentityAction<R>): Result<R, RemoteFailure>

    companion object : AwsServiceCompanion("cognito-identity")
}
