package org.http4k.security

import java.time.Clock
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

fun interface CredentialsProvider<T> : () -> T? {
    companion object
}

fun interface RefreshCredentials<T> : (T?) -> ExpiringCredentials<T>?

data class ExpiringCredentials<T>(val credentials: T, val expiry: Instant)

fun <T> CredentialsProvider.Companion.Refreshing(
    gracePeriod: Duration = Duration.ofSeconds(10),
    clock: Clock = Clock.systemUTC(),
    refreshFn: RefreshCredentials<T>
) = object : CredentialsProvider<T> {
    private val stored = AtomicReference<ExpiringCredentials<T>>(null)

    private val isRefreshing = AtomicReference(false)

    override fun invoke() = (stored.get()
        ?.takeIf {
            val expiresSoon = it.expiresWithin(gracePeriod)
            val expiresReallySoon = it.expiresWithin(gracePeriod.dividedBy(2))
            val isAlreadyRefreshing = isRefreshing.get()

            (!expiresSoon || isAlreadyRefreshing) && !expiresReallySoon
        } ?: refresh())?.credentials

    private fun refresh() = synchronized(this) {
        isRefreshing.set(true)
        try {
            val current = stored.get()
            when {
                current != null && !current.expiresWithin(gracePeriod) -> current
                else -> try {
                    refreshFn(current?.credentials).also(stored::set)
                } catch (e: Exception) {
                    if (current == null || current.expiresWithin(ZERO)) throw e
                    current
                }
            }
        } finally {
            isRefreshing.set(false)
        }
    }

    private fun ExpiringCredentials<T>.expiresWithin(duration: Duration): Boolean =
        expiry
            .minus(duration)
            .isBefore(clock.instant())
}
