package org.http4k.routing

import org.http4k.core.Request

fun Request.path(name: String): String =
    when (val uriTemplate = this.uriTemplate()) {
        null -> throw IllegalStateException("Request was not routed, so no uri-template present")
        else -> uriTemplate.extract(uri.path)[name] ?: throw IllegalArgumentException("No path parameter named '$name'")
    }

