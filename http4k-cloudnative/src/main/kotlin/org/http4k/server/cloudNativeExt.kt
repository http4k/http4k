package org.http4k.server

import org.http4k.config.Port
import org.http4k.core.HttpHandler

fun HttpHandler.asServer(fn: (Int) -> ServerConfig, port: Port) = fn(port.value).toServer(this)
