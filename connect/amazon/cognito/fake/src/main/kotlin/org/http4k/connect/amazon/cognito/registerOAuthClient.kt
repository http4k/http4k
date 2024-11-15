package org.http4k.connect.amazon.cognito

import dev.forkhandles.result4k.valueOrNull
import org.http4k.connect.amazon.cognito.model.ClientId
import org.http4k.connect.amazon.cognito.model.ClientName
import org.http4k.connect.amazon.cognito.model.CloudFrontDomain
import org.http4k.connect.amazon.cognito.model.PoolName
import org.http4k.core.Credentials
import java.util.UUID

/**
 * Helper method to create some OAuth Client credentials inside FakeCognito
 */
fun FakeCognito.registerOAuthClient(id: ClientId, clientName: ClientName = ClientName.of(id.value)): Credentials =
    with(client()) {
        val poolId = createUserPool(PoolName.of("POOL" + UUID.randomUUID())).valueOrNull()!!.UserPool.Id!!

        createResourceServer(poolId, "foo", "bar").valueOrNull()
        createUserPoolDomain(poolId, CloudFrontDomain.of(id.value)).valueOrNull()
        val userPoolClient = createUserPoolClient(
            UserPoolId = poolId,
            ClientName = clientName,
            GenerateSecret = true
        ).valueOrNull()!!.UserPoolClient

        Credentials(userPoolClient.ClientId.value, userPoolClient.ClientSecret!!.value)
    }

