package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then

/**
 * Endpoint security. Provides filter to be applied to endpoints for all requests.
 */
interface Security : Iterable<Security> {
    val filter: Filter
    override fun iterator() = listOf(this).iterator()
}

interface CompositeSecurity : Security {
    companion object
}

fun Security.and(that: Security): CompositeSecurity {
    val all = listOf(this) + that
    return object : CompositeSecurity {
        override fun iterator() = all.iterator()

        override val filter = fold(Filter.NoOp) { acc, next -> acc.then(next.filter) }
    }
}

fun Security.or(that: Security): CompositeSecurity {
    val all = listOf(this) + that

    return object : CompositeSecurity {
        override fun iterator() = all.iterator()

        override val filter = Filter { next ->
            {
                all.asSequence().map { sec -> sec.filter.then(next)(it) }
                    .firstOrNull { it.status != UNAUTHORIZED } ?: Response(UNAUTHORIZED)
            }
        }
    }
}
