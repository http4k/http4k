package org.http4k.security.oauth.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.oauth.format.OAuthMoshi
import org.http4k.security.oauth.metadata.ResourceMetadata
import org.http4k.security.oauth.metadata.ServerMetadata

data class OAuthAuthorizationServer(val serverUri: Uri, val serverMetadata: ServerMetadata)

/**
 * How an authentication client can get the information about the authorization server
 */
fun interface AuthServerDiscovery {
    operator fun invoke(http: HttpHandler): Result<OAuthAuthorizationServer, Exception>

    companion object {
        fun fromKnownAuthServer(serverUri: Uri) = AuthServerDiscovery { http: HttpHandler ->
            val uri = serverUri.path("/.well-known/oauth-authorization-server")
            val response = http(Request(GET, uri))

            when {
                response.status.successful -> resultFrom {
                    OAuthAuthorizationServer(serverUri, OAuthMoshi.asA(response.bodyString(), ServerMetadata::class))
                }

                else -> Failure(Exception("Failed to discover OAuth endpoints from $uri (got ${response.status})"))
            }
        }

        /**
         * Work backwards from the original Url that you are attempting to access
         */
        fun fromProtectedResource(
            resourceUri: Uri,
            retrieveAuthServer: (ResourceMetadata) -> Uri = {
                it.authorizationServers?.first() ?: error("No auth servers")
            }
        ) = AuthServerDiscovery { http: HttpHandler ->
            val uri = resourceUri.path("/.well-known/oauth-protected-resource")
            val response = http(Request(GET, uri))
            when {
                response.status.successful -> Success(OAuthMoshi.asA(response.bodyString(), ResourceMetadata::class))
                else -> Failure(Exception("Failed to discover OAuth endpoints from $uri (got ${response.status})"))
            }
                .flatMap { resultFrom { retrieveAuthServer(it) } }
                .flatMap { fromKnownAuthServer(it)(http) }
        }
    }
}
