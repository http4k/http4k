package org.http4k.routing

import org.http4k.core.Request

fun Request.path(name: String): String? = when (this) {
    is RoutedMessage ->
        (xUriTemplate ?: error("Request was not routed, so no uri-template present"))
            .extract(uri.path)[name]

    else -> error("Request was not routed, so no uri-template present")
}
