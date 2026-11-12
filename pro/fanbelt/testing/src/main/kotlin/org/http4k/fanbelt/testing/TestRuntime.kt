/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.testing.RecordingEvents
import java.util.Random

val NoFakeWired: HttpHandler = { Response(SERVICE_UNAVAILABLE).body("no fake wired for ${it.method} ${it.uri}") }

class TestRuntime(
    override val scheduler: DeterministicScheduler = DeterministicScheduler(start),
    override val events: RecordingEvents = RecordingEvents(),
    override val http: HttpHandler = NoFakeWired,
) : AppRuntime {
    override val clock = SchedulerClock(scheduler)
    override val random = Random(42)
}
