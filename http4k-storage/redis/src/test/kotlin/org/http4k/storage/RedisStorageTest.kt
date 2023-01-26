package org.http4k.storage

import org.http4k.core.Uri
import org.http4k.storage.util.assumeDockerDaemonRunning
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName.parse

@Testcontainers
class RedisStorageTest : StorageContract() {
    init {
        assumeDockerDaemonRunning()
    }

    @Container
    val redis = GenericContainer(parse("redis:7-alpine")).withExposedPorts(6379)

    override val storage by lazy {
        Storage.Redis<AnEntity>(Uri.of("redis://${redis.host}:${redis.firstMappedPort}"))
    }
}

