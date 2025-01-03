package org.http4k.testing

import org.http4k.server.PolyHandler

fun interface PolyAppProvider : () -> PolyHandler
