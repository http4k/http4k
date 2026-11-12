/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import dev.forkhandles.time.DeterministicScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC

class SchedulerClock(private val scheduler: DeterministicScheduler) : Clock() {
    override fun getZone(): ZoneId = UTC

    override fun withZone(zone: ZoneId) = this

    override fun instant(): Instant = scheduler.currentTime()
}

val start: Instant = Instant.ofEpochMilli(0)
