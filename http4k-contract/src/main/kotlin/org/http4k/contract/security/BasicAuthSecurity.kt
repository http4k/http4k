package org.http4k.contract.security

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.filter.ServerFilters
import org.http4k.lens.RequestContextLens

class BasicAuthSecurity private constructor(override val filter: Filter, val name: String = "basicAuth") : Security {
    /**
     * Checks the presence of basic auth credentials. Filter returns 401 if auth fails.
     */
    constructor(realm: String, credentials: Credentials, name: String = "basicAuth"): this(ServerFilters.BasicAuth(realm, credentials), name)

    companion object {
        /**
         * Population of a RequestContext with custom principal object
         */
        operator fun <T> invoke(realm: String, key: RequestContextLens<T>, lookup: (Credentials) -> T?, name: String = "basicAuth") =
            BasicAuthSecurity(ServerFilters.BasicAuth(realm, key, lookup), name)
    }
}
