package org.http4k.connect.amazon.secretsmanager

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import java.util.UUID

class RealSecretsManagerTest : SecretsManagerContract, RealAwsContract {
    override val http = JavaHttpClient()


    override val propogateTime: Long = 5000

    override val nameOrArn = UUID.randomUUID().toString()
}


