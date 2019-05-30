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
    private val authorizationCodeAccessTokenGenerator = AuthorizationCodeAccessTokenGenerator(clientValidator, authorizationCodes, accessTokens, clock, idTokens)

    fun generate(request: Request): Result<AccessTokenDetails, AccessTokenError> {
        grantTypeForm(request).let {
            val grantType = grantType(it)
            return when (grantType) {
                "authorization_code" -> authorizationCodeAccessTokenGenerator.resolveRequest(request).flatMap(authorizationCodeAccessTokenGenerator::generate)
                "client_credentials" -> Failure(UnsupportedGrantType(grantType))
                else -> Failure(UnsupportedGrantType(grantType))
            }
        }
    }

    companion object {
        val grantType = FormField.required("grant_type")
        val grantTypeForm = Body.webForm(Validator.Strict, grantType).toLens()
    }
}

