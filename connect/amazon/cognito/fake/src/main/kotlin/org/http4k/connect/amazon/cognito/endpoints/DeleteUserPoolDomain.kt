package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AmazonJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.DeleteUserPoolDomain
import org.http4k.connect.storage.Storage

fun AmazonJsonFake.deleteUserPoolDomain(pools: Storage<CognitoPool>) = route<DeleteUserPoolDomain> { domain ->
    pools[domain.UserPoolId.value]?.let {
        it.domains -= domain.Domain
    }
}
