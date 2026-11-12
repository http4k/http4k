/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.Events
import org.http4k.fanbelt.events.EventsJson
import java.security.SecureRandom
import java.time.Clock
import java.util.Random
import java.util.concurrent.ScheduledExecutorService

interface AppRuntime : Lifecycle {
    val clock: Clock
    val random: Random
    val events: Events
    val scheduler: ScheduledExecutorService
    val http: HttpHandler

    override fun close() = scheduler.close()

    fun withEvents(events: Events): AppRuntime = AppRuntime(clock, random, events, scheduler, http)

    companion object {
        operator fun invoke(
            clock: Clock = Clock.systemUTC(),
            random: Random = SecureRandom(),
            events: Events = AutoMarshallingEvents(EventsJson),
            scheduler: ScheduledExecutorService = VirtualThreadScheduler(),
            http: HttpHandler = JavaHttpClient(),
        ): AppRuntime = object : AppRuntime {
            override val clock = clock
            override val random = random
            override val events = events
            override val scheduler = scheduler
            override val http = http
        }
    }
}
