package org.http4k.webdriver.datastar

import org.jsoup.nodes.Element

object ScriptNavigation : WatcherPlugin {
    private val assignment = Regex("""^\s*window\.location(?:\.href\s*=\s*|\.assign\(\s*)(['"])(.+?)\1\s*\)?\s*;?\s*$""")

    override fun patched(element: Element, page: DatastarPage) = element.select("script").forEach { script ->
        val location = assignment.find(script.data())?.groupValues?.get(2) ?: throw UnsupportedDatastarExpression("<script>", script.data())
        page.navigate(location)
    }
}
