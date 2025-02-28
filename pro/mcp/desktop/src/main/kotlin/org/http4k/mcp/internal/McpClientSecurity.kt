package org.http4k.mcp.internal

import dev.forkhandles.bunting.MissingFlag
import org.http4k.client.JavaHttpClient
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.NoOp
import org.http4k.core.Uri
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header
import org.http4k.mcp.McpOptions
import org.http4k.security.ContentTypeJsonOrForm
import org.http4k.security.Security
import org.http4k.security.oauth.client.OAuthClientCredentials
import org.http4k.security.oauth.client.RefreshingOAuthToken
import java.time.Clock
import java.time.Duration


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

    class BasicAuth(credentials: Credentials) : McpClientSecurity {
        override val filter = ClientFilters.BasicAuth(credentials)
    }

    class OAuthSecurity(
        tokenUri: Uri,
        oauthCredentials: Credentials,
        scopes: List<String> = emptyList(),
        backend: HttpHandler = JavaHttpClient(),
        clock: Clock = Clock.systemUTC(),
    ) : McpClientSecurity {

        override val filter = ClientFilters.RefreshingOAuthToken(
            oauthCredentials,
            tokenUri,
            backend,
            Duration.ofSeconds(30),
            clock,
            ContentTypeJsonOrForm(),
            scopes,
            ClientFilters.OAuthClientCredentials(oauthCredentials, scopes),
        )
    }

    companion object {
        fun from(options: McpOptions, clock: Clock, http: HttpHandler) = with(options) {
            when {
                apiKey != null -> ApiKey(apiKeyHeader, apiKey!!)
                bearerToken != null -> BearerAuth(bearerToken!!)
                basicAuth != null -> BasicAuth(basicAuth!!)
                oauthTokenUrl != null -> OAuthSecurity(
                    oauthTokenUrl!!,
                    oauthClientCredentials ?: throw MissingFlag(::oauthClientCredentials),
                    oauthScopes,
                    http,
                    clock
                )
                else -> None
            }
        }
    }
}
