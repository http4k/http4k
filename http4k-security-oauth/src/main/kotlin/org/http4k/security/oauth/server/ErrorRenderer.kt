package org.http4k.security.oauth.server

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Header
import org.http4k.security.oauth.server.RfcError.InvalidClient
import org.http4k.security.oauth.server.RfcError.InvalidGrant
import org.http4k.security.oauth.server.RfcError.UnsupportedGrantType


class ErrorRenderer(
    private val json: AutoMarshallingJson,
    private val documentationUri: String = ""
) {

    fun render(response: Response, error: OAuthError) =
        response.with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(json.asJsonString(ErrorResponse(error.rfcError.rfcValue, error.description, documentationUri)))

    private val RfcError.rfcValue
        get() = when (this) {
            InvalidClient -> "invalid_client"
            InvalidGrant -> "invalid_grant"
            UnsupportedGrantType -> "unsupported_grant_type"
        }

    private data class ErrorResponse(val error: String, val error_description: String, val error_uri: String)
}

