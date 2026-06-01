package org.http4k.storyboard.datastar

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
            .forEach { fragment ->
                selectFirst("#${fragment.id()}")?.replaceWith(fragment)
            }
        return
    }

    val target = selectFirst(selector) ?: return
    when (patch.mode) {
        outer, replace -> { target.before(html); target.remove() }
        inner -> target.html(html)
        prepend -> target.prepend(html)
        append -> target.append(html)
        before -> target.before(html)
        after -> target.after(html)
        remove -> target.remove()
    }
}
