package org.http4k.connect.amazon.cognito.oauth

import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.storage.Storage
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientValidator
import org.http4k.security.secureEquals

class CognitoPoolClientValidator(private val storage: Storage<CognitoPool>) : ClientValidator {
    override fun validateClientId(request: Request, clientId: ClientId) =
        storage.keySet().any {
            storage[it]!!.clients.any { it.ClientId.value == clientId.value }
        }

    /**
     * We are non-strict here - we only check the secret if there was one generated. This will differ from
     * real cognito (which would reject if an attempt was made to auth with a client with no secret)
     */
    override fun validateCredentials(request: Request, clientId: ClientId, clientSecret: String) =
        storage.keySet().any {
            storage[it]!!.clients.any {
                it.ClientId.value == clientId.value &&
                    (it.ClientSecret == null || secureEquals(it.ClientSecret?.value, clientSecret))
            }
        }

    override fun validateRedirection(request: Request, clientId: ClientId, redirectionUri: Uri) = true

    override fun validateScopes(request: Request, clientId: ClientId, scopes: List<String>) = true
}
