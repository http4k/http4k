package org.http4k.connect.amazon.dynamodb

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.fakeAwsEnvironment
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.util.PortBasedTest
import org.http4k.util.assumeDockerDaemonRunning
import java.time.Duration
import java.util.UUID

class LocalDynamoTest : DynamoDbContract, PortBasedTest {
    init {
        assumeDockerDaemonRunning()
    }

    override val table = TableName.sample()

    override val duration: Duration get() = Duration.ofSeconds(1)

    override val http by lazy {
        SetBaseUriFrom(localDynamoDbUri)
            .then(JavaHttpClient())
    }

    override fun uuid(seed: Int) = UUID.randomUUID()

    override val aws = fakeAwsEnvironment
}
