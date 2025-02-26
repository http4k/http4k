package org.http4k.mcp.internal

import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.NoOp
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header
import org.http4k.mcp.McpOptions
import org.http4k.security.Security


sealed interface McpClientSecurity : Security {
    data object None : McpClientSecurity {
        override val filter = Filter.NoOp
    }

    class ApiKey(header: String, key: String) : McpClientSecurity {
        override val filter = ClientFilters.ApiKeyAuth(Header.required(header) of key)
    }

    class BearerAuth(token: String) : McpClientSecurity {
        override val filter = ClientFilters.BearerAuth(token)
    }

    class BasicAuth(credentials: String) : McpClientSecurity {
        override val filter =
            ClientFilters.BasicAuth(Credentials(credentials.substringBefore(":"), credentials.substringAfter(":")))
    }

    companion object {
        fun from(options: McpOptions) = with(options) {
            when {
                apiKey != null -> ApiKey(apiKeyHeader, apiKey!!)
                bearerToken != null -> BearerAuth(bearerToken!!)
                basicAuth != null -> BasicAuth(basicAuth!!)
                else -> None
            }
        }
    }
}
