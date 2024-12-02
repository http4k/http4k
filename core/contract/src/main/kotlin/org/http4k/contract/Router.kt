package org.http4k.contract

import org.http4k.core.Request
import org.http4k.routing.RouterDescription

interface Router {
    fun match(request: Request): RouterMatch

    val description: RouterDescription get() = RouterDescription.unavailable
}
