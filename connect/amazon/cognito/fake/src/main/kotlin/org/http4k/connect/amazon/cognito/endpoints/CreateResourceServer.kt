package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.CreateResourceServer
import org.http4k.connect.amazon.cognito.action.CreatedResourceServer
import org.http4k.connect.amazon.cognito.action.ResourceServer
import org.http4k.connect.storage.Storage

fun AwsJsonFake.createResourceServer(pools: Storage<CognitoPool>) = route<CreateResourceServer> {
    pools[it.UserPoolId.value]?.let { pool ->
        pool.resourceServers += it
        CreatedResourceServer(ResourceServer(it.UserPoolId, it.Name, it.Identifier, it.Scopes))
    }
}

