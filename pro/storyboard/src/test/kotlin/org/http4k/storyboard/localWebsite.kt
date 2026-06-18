/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static

internal fun localWebsite(): HttpHandler {
    val pages = Classpath("org/http4k/storyboard/website")
    fun page(name: String) =
        Response(OK).with(CONTENT_TYPE of TEXT_HTML).body(pages.load(name)!!.readText())

    return routes(
        "/" bind GET to { page("index.html") },
        "/pro" bind GET to { page("pro.html") },
        static(Classpath("org/http4k/storyboard"))
    )
}
