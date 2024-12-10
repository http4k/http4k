package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.DeleteUserPool
import org.http4k.connect.storage.Storage

fun AwsJsonFake.deleteUserPool(pools: Storage<CognitoPool>) = route<DeleteUserPool> { pool ->
    pools[pool.UserPoolId.value]?.let {
        pools.remove(pool.UserPoolId.value)
        Unit
    }
}
