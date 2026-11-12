/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.core.Request
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

class Layout(
    val regions: List<Region<*, *>>,
    val renderer: TemplateRenderer,
    val subject: Region<*, *>? = null,
    val signals: Any? = null,
    val shell: (Request, PageShell) -> ViewModel,
)
