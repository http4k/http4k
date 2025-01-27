package org.http4k.contract.security

import org.http4k.core.Filter

/**
 * Default NoOp security filter. Filter allows all traffic through.
 */
object NoSecurity : Security {
    override val filter = Filter { it }
}
