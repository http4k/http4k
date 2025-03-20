package org.http4k.client

import io.helidon.http.HeaderNames

internal fun String.toHelidonHeaderName() = HeaderNames.create(lowercase(), this)
