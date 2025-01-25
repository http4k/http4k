package org.http4k.filter

import org.http4k.server.PolyHandler

fun PolyHandler.debug() = PolyHandler(
    http = http?.debug(),
    ws = ws?.debug(),
    sse = sse?.debug()
)
