package org.http4k.security.oauth.server

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.with
import org.http4k.format.AutoMarshalling
import org.http4k.lens.Header.CONTENT_TYPE

class JsonResponseErrorRenderer(
    private val json: AutoMarshalling,
    private val documentationUri: String? = null
) {
    fun response(error: OAuthError) = when (error) {
        is InvalidClientCredentials -> createResponse(error, Response(UNAUTHORIZED))
        else -> createResponse(error, Response(BAD_REQUEST))
    }

    private fun createResponse(error: OAuthError, response: Response) =
        response.with(CONTENT_TYPE of APPLICATION_JSON)
            .body(
                json.asFormatString(
                    ErrorResponse(
                        error.rfcError.rfcValue,
                        error.description,
                        documentationUri
                    )
                )
            )
}

data class ErrorResponse(val error: String, val error_description: String, val error_uri: String?)
