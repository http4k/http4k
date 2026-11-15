/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.fanbelt.input.goTo

/**
 * How a viewer leaves a page whose rule refuses them: page answers the first paint with the refusal's status, and live
 * is the script sent to a page that is already open.
 */
interface Exit {
    fun page(request: Request, refusal: Status): Response

    fun live(request: Request): String

    /**
     * Answers the first paint with the refusal, and reloads an open page so its viewer meets it.
     */
    data object Reload : Exit {
        override fun page(request: Request, refusal: Status) = Response(refusal)

        override fun live(request: Request) = "<script>window.location.reload()</script>"
    }

    /**
     * Sends the viewer to a location, whether they arrive or are already watching.
     */
    class GoTo(private val where: (Request) -> Uri) : Exit {
        constructor(location: Uri) : this({ location })

        override fun page(request: Request, refusal: Status) = Response(SEE_OTHER).header("Location", where(request).toString())

        override fun live(request: Request) = goTo(where(request))
    }
}
