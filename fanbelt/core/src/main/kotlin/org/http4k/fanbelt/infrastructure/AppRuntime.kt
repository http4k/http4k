package org.http4k.fanbelt.infrastructure

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.events.AutoMarshallingEvents
import org.http4k.events.EventFilters
import org.http4k.events.Events
import org.http4k.events.then
import org.http4k.fanbelt.events.EventsJson
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Random
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * The clock, randomness, events, scheduler and outbound HTTP client an app uses. Tests pass a TestRuntime.
 */
interface AppRuntime : Lifecycle {
    val clock: Clock
    val random: Random
    val events: Events
    val scheduler: ScheduledExecutorService
    val http: HttpHandler

    override fun close() = scheduler.close()

    fun withEvents(events: Events) = object : AppRuntime by this {
        override val events = events
    }

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
            override val events = EventFilters.AddTimestamp(clock).then(EventFilters.AddEventName()).then(events)
            override val scheduler = scheduler
            override val http = http
        }
    }
}

internal fun AppRuntime.at(at: Instant, task: () -> Unit): ScheduledFuture<*> {
    val now = clock.instant()
    return scheduler.schedule(task, Duration.between(now, maxOf(at, now)).toNanos(), NANOSECONDS)
}
