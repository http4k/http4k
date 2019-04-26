package org.http4k.security.oauth.server

import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.lens.Header


class ErrorRenderer(
    private val json: AutoMarshallingJson,
    private val documentationUri: String = ""
) {

    fun render(response: Response, error: String, errorDescription: String) =
        response.with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
            .body(json.asJsonString(ErrorResponse(error, errorDescription, documentationUri)))
}

