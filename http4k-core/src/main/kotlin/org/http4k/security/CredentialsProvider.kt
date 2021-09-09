package org.http4k.security

import java.time.Clock
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

fun interface CredentialsProvider<T> : () -> T? {
    companion object
}

data class ExpiringCredentials<T>(val credentials: T, val expiry: Instant)

fun <T> CredentialsProvider.Companion.Refreshing(
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC(),
    refreshFn: CredentialsProvider<ExpiringCredentials<T>>
) = object : CredentialsProvider<T> {
    private val stored = AtomicReference<ExpiringCredentials<T>>(null)

    override fun invoke() = (stored.get()?.takeUnless { it.expiresWithin(gracePeriod) } ?: refresh())?.credentials

    private fun refresh(): ExpiringCredentials<T>? =
        synchronized(stored) {
            val current = stored.get()
            when {
                current != null && !current.expiresWithin(gracePeriod) -> current
                else -> try {
                    val newCreds = refreshFn()
                    stored.set(newCreds)
                    newCreds
                } catch (e: Exception) {
                    if (current == null || current.expiresWithin(ZERO)) throw e
                    current
                }
            }
        }

    private fun ExpiringCredentials<T>.expiresWithin(duration: Duration): Boolean =
        expiry
            .minus(duration)
            .isBefore(clock.instant())
}
