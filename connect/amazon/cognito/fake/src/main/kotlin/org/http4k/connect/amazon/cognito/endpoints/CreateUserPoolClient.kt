package org.http4k.connect.amazon.cognito.endpoints

import dev.forkhandles.values.ZERO
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.action.CreateUserPoolClient
import org.http4k.connect.amazon.cognito.action.CreatedUserPoolClient
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ClientSecret
import org.http4k.connect.amazon.cognito.model.TokenValidityUnits
import org.http4k.connect.amazon.cognito.model.UserPoolClient
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage

fun AwsJsonFake.createUserPoolClient(pools: Storage<CognitoPool>) = route<CreateUserPoolClient> { client ->
    pools[client.UserPoolId.value]?.let { pool ->
        val newClient = UserPoolClient(
            ClientId.of(client.ClientName.value),
            client.ClientName,
            client.UserPoolId,
            Timestamp.ZERO,
            Timestamp.ZERO,
            3600,
            TokenValidityUnits(),
            client.AllowedOAuthFlowsUserPoolClient,
            ClientSecret = if (client.GenerateSecret == true) ClientSecret.of(client.ClientName.value.reversed()) else null
        )
        pool.clients += newClient
        CreatedUserPoolClient(newClient)
    }
}
