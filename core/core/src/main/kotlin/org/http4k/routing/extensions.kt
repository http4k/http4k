package org.http4k.routing

import org.http4k.core.Request

fun Request.path(name: String): String? = when (this) {
    is RoutedMessage -> xUriTemplate?.extract(uri.path)?.get(name) ?: throw IllegalStateException("Request was not routed, so no uri-template present")
    else -> throw IllegalStateException("Request was not routed, so no uri-template present")
}

