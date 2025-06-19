package org.http4k.testing

import org.http4k.core.PolyHandler
import org.http4k.core.Request

fun PolyHandler.testHttpClient(request: Request) = http?.invoke(request) ?: error("No HTTP handler set.")
