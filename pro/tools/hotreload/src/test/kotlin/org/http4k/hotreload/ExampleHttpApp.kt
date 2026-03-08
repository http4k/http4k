/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.hotreload

import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

class ExampleHttpApp : HotReloadable<HttpHandler> {
    override fun create() = { req: Request -> Response(Status.OK).body("asdsassd") }
}
