package org.http4k.contract.security

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.filter.ServerFilters

/**
 * Checks the presence of basic auth credentials. Filter returns 401 if auth fails.
 */
class BasicAuthSecurity(realm: String, credentials: Credentials, val name: String = "basicAuth") : Security {
    override val filter: Filter = ServerFilters.BasicAuth(realm, credentials)

    companion object
}
