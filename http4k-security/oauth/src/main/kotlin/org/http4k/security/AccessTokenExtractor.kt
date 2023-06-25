package org.http4k.security

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.lens.Header
import org.http4k.security.OAuthCallbackError.CouldNotFetchAccessToken
import org.http4k.security.OAuthWebForms.responseForm
import org.http4k.security.oauth.format.OAuthMoshi.auto
import org.http4k.security.openid.IdToken

fun interface AccessTokenExtractor : (Response) -> Result<AccessTokenDetails, CouldNotFetchAccessToken>

/**
 * Extracts a standard OAuth access token from JSON or Form encoded response.
 */
class ContentTypeJsonOrForm : AccessTokenExtractor {
    override fun invoke(msg: Response) =
        resultFrom {
            Header.CONTENT_TYPE(msg)
                ?.let {
                    when {
                        APPLICATION_JSON.equalsIgnoringDirectives(it) ->
                            with(accessTokenResponseBody(msg)) {
                                AccessTokenDetails(toAccessToken(), id_token?.let(::IdToken))
                            }

                        APPLICATION_FORM_URLENCODED.equalsIgnoringDirectives(it) -> responseForm(msg)
                        else -> null
                    }
                } ?: AccessTokenDetails(AccessToken(msg.bodyString()))
        }.mapFailure { CouldNotFetchAccessToken(msg.status, msg.bodyString()) }
}

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()
