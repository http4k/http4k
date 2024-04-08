package org.http4k.security

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Header
import org.http4k.security.OAuthCallbackError.CouldNotFetchAccessToken
import org.http4k.security.OAuthWebForms.responseForm
import org.http4k.security.oauth.format.OAuthMoshi
import org.http4k.security.oauth.format.OAuthMoshi.auto
import org.http4k.security.openid.IdToken

fun interface AccessTokenExtractor : (Response) -> Result<AccessTokenDetails, CouldNotFetchAccessToken>

/**
 * Extracts a standard OAuth access token from JSON or Form encoded response.
 */
class ContentTypeJsonOrForm(
    private val autoMarshallingJson: AutoMarshallingJson<*> = OAuthMoshi
) : AccessTokenExtractor {
    override fun invoke(response: Response) =
        resultFrom {
            when (Header.CONTENT_TYPE(response)?.value) {
                APPLICATION_JSON.value ->
                    autoMarshallingJson.asA<AccessTokenResponse>(response.bodyString())
                        .let { AccessTokenDetails(it.toAccessToken(), it.id_token?.let(::IdToken)) }
                APPLICATION_FORM_URLENCODED.value -> responseForm(response)
                else -> AccessTokenDetails(AccessToken(response.bodyString()))
            }
        }.mapFailure { CouldNotFetchAccessToken(response.status, response.bodyString()) }
}

val accessTokenResponseBody = Body.auto<AccessTokenResponse>().toLens()
