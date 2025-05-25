package org.http4k.filter

import org.http4k.core.PolyHandler
import java.io.PrintStream

suspend fun PolyHandler.debug(out: PrintStream = System.out, debugStream: Boolean = false) = PolyHandler(
    http = http?.debug(out, debugStream),
    ws = ws?.debug(out, debugStream),
    sse = sse?.debug(out, debugStream)
)
