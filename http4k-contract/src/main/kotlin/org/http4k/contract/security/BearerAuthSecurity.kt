package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextLens

/**
 * Checks the presence of bearer auth credentials
 */
class BearerAuthSecurity private constructor(override val filter: Filter, val name: String = "bearerAuth") : Security {
    constructor(token: String) : this(ServerFilters.BearerAuth(token))
    constructor(token: (String) -> Boolean) : this(ServerFilters.BearerAuth(token))
    constructor(key: RequestContextLens<Any>, lookup: (String) -> Any?) : this(ServerFilters.BearerAuth(key, lookup))
}