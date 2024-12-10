package org.http4k.connect.amazon.dynamodb

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.fakeAwsEnvironment
import org.http4k.connect.assumeDockerDaemonRunning
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.util.PortBasedTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.util.UUID

@Testcontainers
class LocalDynamoTest : DynamoDbContract, PortBasedTest {
    init {
        assumeDockerDaemonRunning()
    }

    override val table = TableName.sample()

    override val duration: Duration get() = Duration.ofSeconds(1)

    override val http by lazy {
        SetBaseUriFrom(Uri.of("http://localhost:${dynamo.getMappedPort(8000)}"))
            .then(JavaHttpClient())
    }

    override fun uuid(seed: Int) = UUID.randomUUID()

    @Container
    val dynamo = GenericContainer(dynamoDbLocalDockerImageName).withExposedPorts(8000)

    override val aws = fakeAwsEnvironment
}
