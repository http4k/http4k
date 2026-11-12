/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.webdriver.datastar.patched

import org.http4k.webdriver.datastar.DatastarPage
import org.http4k.webdriver.datastar.UnsupportedDatastarExpression
import org.http4k.webdriver.datastar.WatcherPlugin
import org.jsoup.nodes.Element

object ScriptNavigation : WatcherPlugin {
    private val assignment = Regex("""^\s*window\.location(?:\.href\s*=\s*|\.assign\(\s*)(['"])(.+?)\1\s*\)?\s*;?\s*$""")

    override fun patched(element: Element, page: DatastarPage) = element.select("script").forEach { script ->
        val location = assignment.find(script.data())?.groupValues?.get(2) ?: throw UnsupportedDatastarExpression("<script>", script.data())
        page.navigate(location)
    }
}
