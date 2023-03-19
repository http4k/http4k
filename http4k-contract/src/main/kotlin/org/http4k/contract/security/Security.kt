package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then

/**
 * Endpoint security. Provides filter to be applied to endpoints for all requests.
 */
interface Security {
    val filter: Filter

    companion object {
        val None: Security? = null
    }
}

fun Security.and(that: Security): Security = when (this) {
    is AndSecurity -> AndSecurity(this + that)
    else -> AndSecurity(listOf(this) + that)
}

internal data class AndSecurity(internal val all: List<Security>) : Security, Iterable<Security> {
    override fun iterator() = all.iterator()

    override val filter = all.fold(Filter.NoOp) { acc, next -> acc.then(next.filter) }
}

fun Security.or(that: Security): Security = when (this) {
    is OrSecurity -> OrSecurity(this + that)
    else -> OrSecurity(listOf(this) + that)
}

internal data class OrSecurity(internal val all: List<Security>) : Security, Iterable<Security> {
    override fun iterator() = all.iterator()

    override val filter = Filter { next ->
        {
            all.asSequence().map { sec -> sec.filter.then(next)(it) }
                .firstOrNull { it.status != UNAUTHORIZED } ?: Response(UNAUTHORIZED)
        }
    }
}
