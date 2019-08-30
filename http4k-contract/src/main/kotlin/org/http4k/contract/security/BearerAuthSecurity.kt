package org.http4k.contract.security

import org.http4k.core.Filter
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextLens

/**
 * Checks the presence of bearer auth credentials. Filter returns 401 if auth fails.
 */
class BearerAuthSecurity private constructor(override val filter: Filter, val name: String = "bearerAuth") : Security {
    constructor(token: String, name: String = "bearerAuth") : this(ServerFilters.BearerAuth(token), name)
    constructor(token: (String) -> Boolean, name: String = "bearerAuth") : this(ServerFilters.BearerAuth(token), name)
    constructor(key: RequestContextLens<Any>, lookup: (String) -> Any?, name: String = "bearerAuth") : this(ServerFilters.BearerAuth(key, lookup), name)

    companion object
}