package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.CreateUserPool
import org.http4k.connect.amazon.cognito.action.CreatedUserPool
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.cognito.model.UserPoolType
import org.http4k.connect.storage.Storage
import java.util.UUID

fun AwsJsonFake.createUserPool(pools: Storage<CognitoPool>) = route<CreateUserPool> {
    val id = UUID.nameUUIDFromBytes(it.PoolName.value.toByteArray()).toString()
    pools[id] = CognitoPool(it.PoolName)

    CreatedUserPool(UserPoolType(Id = UserPoolId.of(id)))
}

