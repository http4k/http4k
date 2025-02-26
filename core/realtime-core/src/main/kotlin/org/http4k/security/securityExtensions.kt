package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.sse.SseFilter

fun Security.then(poly: PolyHandler) = Filter(this).then(SseFilter(this).then(poly))
