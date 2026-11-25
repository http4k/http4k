package org.http4k.fanbelt.testing.infrastructure

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.testing.RecordingEvents
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.util.Random

val NoFakeWired: HttpHandler = { Response(SERVICE_UNAVAILABLE).body("no fake wired for ${it.method} ${it.uri}") }

/**
 * A deterministic AppRuntime: a DeterministicScheduler from the epoch, seeded randomness, recorded events, and outbound
 * HTTP that answers 503. Closing it leaves the scheduler running, as the nodes of an InMemoryCluster share it.
 */
class TestRuntime(
    override val scheduler: DeterministicScheduler = DeterministicScheduler(ofEpochMilli(0)),
    override val events: RecordingEvents = RecordingEvents(),
    override val http: HttpHandler = NoFakeWired,
) : AppRuntime {
    override val clock: Clock = scheduler.toClock()
    override val random: Random = Random(0)

    override fun close() {}
}
