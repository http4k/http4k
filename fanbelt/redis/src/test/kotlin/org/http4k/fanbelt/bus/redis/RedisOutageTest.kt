package org.http4k.fanbelt.bus.redis

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.lessThan
import org.http4k.core.Uri
import org.http4k.events.Event
import org.http4k.fanbelt.bus.BackplaneSubscribeDelayed
import org.http4k.fanbelt.bus.Interest
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

class RedisOutageTest {
    @Test
    fun `a subscribe while redis doesn't answer waits about a second, reports the delay, and carries on`() {
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            addExposedPort(6379)
            start()
        }
        val events = CopyOnWriteArrayList<Event>()
        val transport = RedisTransport(Uri.of("redis://${redis.host}:${redis.getMappedPort(6379)}"), { events += it })
        try {
            transport.start({}, {})
            redis.dockerClient.pauseContainerCmd(redis.containerId).exec()

            val started = System.nanoTime()
            transport.interested(Interest.of("acme"))
            val waited = Duration.ofNanos(System.nanoTime() - started)

            assertThat(waited, lessThan(Duration.ofSeconds(3)))
            assertThat(events.toList(), equalTo(listOf<Event>(BackplaneSubscribeDelayed(Interest.of("acme")))))
        } finally {
            redis.dockerClient.unpauseContainerCmd(redis.containerId).exec()
            transport.close()
            redis.stop()
        }
    }
}
