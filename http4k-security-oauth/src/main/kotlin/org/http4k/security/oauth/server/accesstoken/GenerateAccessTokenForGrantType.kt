package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.flatMap
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.*
import java.time.Clock

class GenerateAccessTokenForGrantType(
    clientValidator: ClientValidator,
    authorizationCodes: AuthorizationCodes,
    accessTokens: AccessTokens,
    clock: Clock,
    idTokens: IdTokens
) {
    private val authorizationCode = AuthorizationCodeAccessTokenGenerator(clientValidator, authorizationCodes, accessTokens, clock, idTokens)
    private val clientCredentials = ClientCredentialsAccessTokenGenerator(accessTokens)

    fun generate(request: Request): Result<AccessTokenDetails, AccessTokenError> {
        grantTypeForm(request).let {
            val grantType = grantType(it)
            return when (grantType) {
                authorizationCode.rfcGrantType ->
                    authorizationCode.resolveRequest(request).flatMap(authorizationCode::generate)
                clientCredentials.rfcGrantType ->
                    clientCredentials.resolveRequest(request).flatMap(clientCredentials::generate)
                else -> Failure(UnsupportedGrantType(grantType))
            }
        }
    }

    companion object {
        val grantType = FormField.required("grant_type")
        val grantTypeForm = Body.webForm(Validator.Strict, grantType).toLens()
    }
}

