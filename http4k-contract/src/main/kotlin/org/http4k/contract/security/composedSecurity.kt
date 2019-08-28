package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then

class AndSecurity(first: Security, vararg rest: Security) : Security {
    private val all = listOf(first) + rest

    override fun iterator() = all.iterator()

    override val filter = this
        .fold(Filter.NoOp) { acc, next -> acc.then(next.filter) }

    companion object
}

class OrSecurity(first: Security, vararg rest: Security) : Security {
    private val all = listOf(first) + rest

    override fun iterator() = all.iterator()

    override val filter = Filter { next ->
        {
            all.asSequence().map { sec -> sec.filter.then(next)(it) }
                .firstOrNull { it.status != UNAUTHORIZED } ?: Response(UNAUTHORIZED)
        }
    }

    companion object
}