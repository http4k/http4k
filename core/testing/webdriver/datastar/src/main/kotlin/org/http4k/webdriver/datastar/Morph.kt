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
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

internal fun Document.applyPatch(patch: PatchElements) {
    val html = patch.elements.joinToString("\n") { it.value }
    val fragment = Jsoup.parseBodyFragment(html).body()
    val selector = patch.selector?.value

    if (selector == null) {
        fragment.children()
            .filter { it.id().isNotEmpty() }
            .forEach { selectFirst("#${it.id()}")?.morphTo(it) }
    } else {
        val target = selectFirst(selector) ?: return

        with(target) {
            when (patch.mode) {
                outer -> fragment.firstElementChild()?.let(::morphTo)

                replace -> {
                    before(html)
                    remove()
                }

                inner -> morphChildrenTo(fragment)

                prepend -> prepend(html)

                append -> append(html)

                before -> before(html)

                after -> after(html)

                remove -> remove()
            }
        }
    }
}

private fun Element.morphTo(new: Element) {
    val morphed = morphed(new)
    if (morphed !== this) replaceWith(morphed)
}

private fun Node.morphed(new: Node): Node = when {
    this is Element && hasAttr("data-ignore-morph") -> this

    this is Element && new is Element && tagName() == new.tagName() -> apply {
        attributes().map { it.key }.filterNot(new::hasAttr).forEach(::removeAttr)
        new.attributes().forEach { attr(it.key, it.value) }
        morphChildrenTo(new)
    }

    this is TextNode && new is TextNode -> apply { text(new.wholeText) }

    else -> new
}

private fun Element.morphChildrenTo(new: Element) {
    val byId = children().filter { it.id().isNotEmpty() }.associateBy { it.id() }
    val positional = childNodes().filterNot { it is Element && it.id().isNotEmpty() }.toMutableList()

    val morphed = new.childNodes().toList().map { newChild ->
        val match = (newChild as? Element)?.id()?.takeIf { it.isNotEmpty() }?.let(byId::get)
            ?: positional.firstOrNull { it.sameKindAs(newChild) }?.also(positional::remove)
        match?.morphed(newChild) ?: newChild
    }

    empty()
    appendChildren(morphed)
}

private fun Node.sameKindAs(other: Node) = when {
    this is TextNode -> other is TextNode
    this is Element && other is Element -> tagName() == other.tagName() && other.id().isEmpty()
    else -> false
}
