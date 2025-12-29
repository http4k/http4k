package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.RevokeToken
import org.http4k.connect.storage.Storage

fun AwsJsonFake.revokeToken(pools: Storage<CognitoPool>) = route<RevokeToken> {
    Unit
}
