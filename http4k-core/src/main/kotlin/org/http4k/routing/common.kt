package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.METHOD_NOT_ALLOWED
import org.http4k.core.Status.Companion.NOT_FOUND

internal val routeNotFoundHandler: HttpHandler = { Response(NOT_FOUND.description("Route not found")) }

internal val routeMethodNotAllowedHandler: HttpHandler = { Response(METHOD_NOT_ALLOWED.description("Method not allowed")) }

