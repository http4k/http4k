package org.http4k.contract.security

import org.http4k.core.Filter

/**
 * Endpoint security. Provides filter to be applied to endpoints for all requests.
 */
interface Security {
    val filter: Filter
}