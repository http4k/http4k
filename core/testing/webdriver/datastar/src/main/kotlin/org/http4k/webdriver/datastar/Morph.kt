package org.http4k.webdriver.datastar

import org.http4k.datastar.DatastarEvent.PatchElements
import org.http4k.datastar.MorphMode.after
import org.http4k.datastar.MorphMode.append
import org.http4k.datastar.MorphMode.before
import org.http4k.datastar.MorphMode.inner
import org.http4k.datastar.MorphMode.outer
import org.http4k.datastar.MorphMode.prepend
import org.http4k.datastar.MorphMode.remove
import org.http4k.datastar.MorphMode.replace
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

internal fun Document.applyPatch(patch: PatchElements) {
    val html = patch.elements.joinToString("\n") { it.value }
    val selector = patch.selector?.value

    if (selector == null) {
        Jsoup.parseBodyFragment(html).body().children()
            .filter { it.id().isNotEmpty() }
            .forEach { selectFirst("#${it.id()}")?.replaceWith(it) }
    } else {
        val target = selectFirst(selector) ?: return

        with(target) {
            when (patch.mode) {
                outer, replace -> {
                    before(html)
                    remove()
                }

                inner -> html(html)
                prepend -> prepend(html)
                append -> append(html)
                before -> before(html)
                after -> after(html)
                remove -> remove()
            }
        }
    }
}
