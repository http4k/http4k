package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.CreateUserPoolDomain
import org.http4k.connect.amazon.cognito.action.CreatedUserPoolDomain
import org.http4k.connect.storage.Storage

fun AwsJsonFake.createUserPoolDomain(pools: Storage<CognitoPool>) = route<CreateUserPoolDomain> {
    pools[it.UserPoolId.value]?.let { pool ->
        pool.domains += it.Domain
        CreatedUserPoolDomain(it.Domain)
    }
}
