package org.http4k.fanbelt.testing.infrastructure

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.testing.RecordingEvents
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.util.Random

/**
 * A deterministic AppRuntime for tests.
 */
class TestRuntime(
    override val scheduler: DeterministicScheduler = DeterministicScheduler(ofEpochMilli(0)),
    override val events: RecordingEvents = RecordingEvents(),
    override val http: HttpHandler = { Response(NOT_IMPLEMENTED) },
) : AppRuntime {
    override val clock: Clock = scheduler.toClock()
    override val random: Random = Random(0)

    override fun close() {}
}
