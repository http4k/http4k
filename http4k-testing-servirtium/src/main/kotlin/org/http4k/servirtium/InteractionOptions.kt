package org.http4k.servirtium

import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response

interface InteractionOptions {
    fun requestManipulations(request: Request): Request = request
    fun responseManipulations(response: Response): Response = response
    fun isBinary(contentType: ContentType): Boolean = false

    companion object {
        object Defaults : InteractionOptions
    }
}
