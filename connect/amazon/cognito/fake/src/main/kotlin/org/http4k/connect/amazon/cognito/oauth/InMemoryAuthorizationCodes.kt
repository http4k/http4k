package org.http4k.connect.amazon.cognito.oauth

import dev.forkhandles.result4k.Success
import org.http4k.base64Encode
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.body.form
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodeDetails
import org.http4k.security.oauth.server.AuthorizationCodes
import java.time.Clock
import java.time.temporal.ChronoUnit.DAYS

class InMemoryAuthorizationCodes(private val clock: Clock) : AuthorizationCodes {
    private val inFlightCodes = mutableMapOf<AuthorizationCode, AuthorizationCodeDetails>()

    override fun detailsFor(code: AuthorizationCode): AuthorizationCodeDetails =
        inFlightCodes[code]?.also { inFlightCodes -= code } ?: error("code not stored")

    override fun create(request: Request, authRequest: AuthRequest, response: Response) =
        Success(AuthorizationCode(request.form(Form.email.meta.name)!!.base64Encode()).also {
            inFlightCodes[it] = AuthorizationCodeDetails(
                authRequest.client, authRequest.redirectUri!!, clock.instant().plus(1, DAYS), null, false,
                authRequest.responseType
            )
        })
}
