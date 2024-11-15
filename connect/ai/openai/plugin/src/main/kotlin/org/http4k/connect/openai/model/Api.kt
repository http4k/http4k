package org.http4k.connect.openai.model

import org.http4k.core.Uri

internal data class Api(val url: Uri, val is_user_authenticated: Boolean) {
    val type = "openapi"
}
