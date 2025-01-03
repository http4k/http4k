package org.http4k.testing

import org.http4k.core.HttpHandler

fun interface HttpAppProvider : () -> HttpHandler
