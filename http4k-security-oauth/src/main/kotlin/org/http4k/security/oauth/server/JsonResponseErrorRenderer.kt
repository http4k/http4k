package org.http4k.security.oauth.server

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Header


class JsonResponseErrorRenderer(
    private val json: AutoMarshallingJson<*>,
    private val documentationUri: String? = null
) {

    fun response(error: OAuthError) = when (error) {
        is InvalidClientCredentials -> createResponse(error, Response(UNAUTHORIZED))
        else -> createResponse(error, Response(BAD_REQUEST))
    }

    private fun createResponse(error: OAuthError, response: Response) =
        response.with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(json.asFormatString(ErrorResponse(
                error.rfcError.rfcValue,
                error.description,
                documentationUri
            )))

    private data class ErrorResponse(val error: String, val error_description: String, val error_uri: String?)
}

