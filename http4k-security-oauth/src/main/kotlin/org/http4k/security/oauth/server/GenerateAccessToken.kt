package org.http4k.security.oauth.server

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.webForm

class GenerateAccessToken(
    private val authorizationCodes: AuthorizationCodes,
    private val accessTokens: AccessTokens) : HttpHandler {

        override fun invoke(request: Request): Response {
                val authorizationCode = authorizationCode(form(request))
                return Response(Status.OK).body(accessTokens.create(authorizationCode).value).also {
                        authorizationCodes.destroy(authorizationCode)
                }
        }

        companion object {
                private val authorizationCode = FormField.map(::AuthorizationCode, AuthorizationCode::value).required("code")

                val form = Body.webForm(Validator.Strict, authorizationCode).toLens()
        }
}