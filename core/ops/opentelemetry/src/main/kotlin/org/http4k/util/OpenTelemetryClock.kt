package org.http4k.util

import io.opentelemetry.sdk.common.Clock

/**
 * Shim to convert java.time.Clock to io.opentelemetry.sdk.common.Clock
 */
fun OpenTelemetryClock(clock: java.time.Clock) = object : Clock {
    override fun now() = clock.instant().toEpochMilli()

    override fun nanoTime() = clock.instant().run { epochSecond * 1_000_000_000L + nano }
}
