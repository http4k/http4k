/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

/**
 * What a page draws: its regions, renderer, subject region (absent means 404), signals class and shell view, given the
 * request, the shell and whoever the page's viewing let in.
 */
class Layout<in Viewer>(
    internal val regions: List<Region<*, *>>,
    internal val renderer: TemplateRenderer,
    internal val subject: Region<*, *>? = null,
    internal val signals: Any? = null,
    internal val shell: (Request, PageShell, Viewer) -> ViewModel,
) {
    init {
        require(subject == null || subject in regions) { "A layout's subject must be one of its regions: ${subject?.id}" }
    }
}

/**
 * A layout whose shell view needs no viewer, for a page with any viewing.
 */
fun Layout(
    regions: List<Region<*, *>>,
    renderer: TemplateRenderer,
    subject: Region<*, *>? = null,
    signals: Any? = null,
    shell: (Request, PageShell) -> ViewModel,
): Layout<Any?> = Layout(regions, renderer, subject, signals) { request, pageShell, viewer -> shell(request, pageShell) }
