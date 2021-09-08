package org.http4k.security

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

fun interface CredentialsProvider<T> : () -> T {
    companion object
}

data class ExpiringCredentials<T>(val credentials: T, val expiry: Instant)

fun <T> CredentialsProvider.Companion.Refreshing(
    refreshFn: CredentialsProvider<ExpiringCredentials<T>>,
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC()
) = object : CredentialsProvider<T> {
    private val stored = AtomicReference<ExpiringCredentials<T>>(null)

    override fun invoke(): T = (stored.get()?.takeIf { !it.expiresWithin(gracePeriod) } ?: refresh()).credentials

    private fun refresh() =
        synchronized(stored) {
            val current = stored.get()
            when {
                current != null && !current.expiresWithin(gracePeriod) -> current
                else -> try {
                    stored.set(refreshFn())
                    stored.get()
                } catch (e: Exception) {
                    if (current.expiresWithin(Duration.ZERO)) throw e
                    stored.get()
                }
            }
        }

    private fun ExpiringCredentials<T>.expiresWithin(duration: Duration): Boolean =
        expiry
            .minus(duration)
            .isBefore(clock.instant())
}
