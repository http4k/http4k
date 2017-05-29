package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request

interface Router {
    fun match(request: Request): HttpHandler?
}