package org.http4k.connect.openfga

import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.util.assumeDockerDaemonRunning
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class RealOpenFgaTest : OpenFgaContract {
    init {
        assumeDockerDaemonRunning()
    }

    override val http = JavaHttpClient()

    override val baseUri get() = Uri.of("http://${openFgaServer.host}:${openFgaServer.firstMappedPort}")

    companion object {
        @Container
        @JvmStatic
        val openFgaServer: GenericContainer<*> = GenericContainer(DockerImageName.parse("openfga/openfga:v1.22.1"))
            .withCommand("run")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/healthz").forPort(8080))
    }
}
