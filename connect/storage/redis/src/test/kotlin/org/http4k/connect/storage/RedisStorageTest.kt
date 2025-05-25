package org.http4k.connect.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.CommandArgs
import io.mockk.every
import io.mockk.mockk
import org.http4k.connect.assumeDockerDaemonRunning
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Testcontainers
class RedisStorageTest : StorageContract() {

    init {
        assumeDockerDaemonRunning()
    }

    @Container
    val redis = GenericContainer(DockerImageName.parse("redis:6.0.10-alpine")).withExposedPorts(6379)

    override val storage: Storage<AnEntity> by lazy {
        Storage.Redis<AnEntity>(Uri.of("redis://${redis.host}:${redis.firstMappedPort}"))
    }

    @Test
    fun `when a custom duration function is passed, the returned value is set as the expiry`() = runBlocking {
        val redisCommands = mockk<RedisCommands<String, AnEntity>>()
        val capturedArgs = mutableListOf<SetArgs>()
        every { redisCommands.set(any(), any(), capture(capturedArgs)) } returns "OK"

        val redisStorage = Storage.RedisWithDynamicTtl<AnEntity>(redisCommands) {
            when (it.name) {
                "sec" -> Duration.ofSeconds(1)
                "min" -> Duration.ofMinutes(2)
                else -> Duration.ofHours(3)
            }
        }

        redisStorage["x"] = AnEntity("sec")
        redisStorage["y"] = AnEntity("hours")
        redisStorage["z"] = AnEntity("min")

        val durations = capturedArgs.map {
            val args = CommandArgs(StringCodec())
            it.build(args)
            args.toCommandString()
        }

        assertThat(durations[0], equalTo("EX 1"))
        assertThat(durations[1], equalTo("EX 10800"))
        assertThat(durations[2], equalTo("EX 120"))
    }
}
