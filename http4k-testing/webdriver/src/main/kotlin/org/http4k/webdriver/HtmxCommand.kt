package org.http4k.webdriver

import org.http4k.core.Method
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

data class HtmxCommand(
    val method: Method,
    val uri: String,
    val target: Element,
    val swap: HtmxSwap,
) {

    fun performOn(element: HtmxJsoupWebElement) {
        val response = element.handler(org.http4k.core.Request(method, uri))
        val responseBody = Jsoup.parse(response.bodyString()).getElementsByTag("body").first()

        swap
            .performSwap(
                element = target,
                newElements = responseBody?.childNodes() ?: emptyList()
            )
    }

    companion object {

        private fun withDataPrefix(p: Pair<String, Method>): List<Pair<String, Method>> =
            listOf(p, "data-${p.first}" to p.second)

        private val hxAttrs = listOf(
            "hx-get" to Method.GET,
            "hx-post" to Method.POST,
            "hx-delete" to Method.DELETE,
            "hx-patch" to Method.PATCH,
            "hx-put" to Method.PUT,
        ).flatMap(::withDataPrefix)

        fun from(element: HtmxJsoupWebElement): HtmxCommand? =
            fromElement(element.delegate.element)

        private fun Element.hxAttr(key: String): String? =
            this.attr("hx-$key").takeIf { it.isNotEmpty() }
                ?: this.attr("data-hx-$key").takeIf { it.isNotEmpty() }


        private fun <T> Element.findInheritedValue(f: (Element) -> T?): T? =
            f(this) ?: this.parents().firstNotNullOfOrNull { f(it) }

        private fun targetElement(element: Element): Element? =
            element
                .hxAttr("target")
                ?.let {
                    when {
                        it == "this" -> element
                        else -> element.ownerDocument()?.getElementById(it)
                    }
                }

        private fun swap(element: Element): HtmxSwap? =
            element
                .hxAttr("swap")
                ?.let(::swapFromString)

        private fun swapFromString(s: String): HtmxSwap? =
            HtmxSwap
                .entries
                .firstOrNull { s.lowercase().startsWith(it.toString().lowercase()) }

        private fun fromElement(element: Element): HtmxCommand? =
            hxAttrs
                .firstOrNull { element.hasAttr(it.first) }
                ?.let {
                    HtmxCommand(
                        method = it.second,
                        uri = element.attr(it.first),
                        target = element.findInheritedValue(::targetElement) ?: element,
                        swap = element.findInheritedValue(::swap) ?: HtmxSwap.InnerHtml,
                    )
                }
    }
}

interface HtmxSwapAction {
    fun performSwap(element: Element, newElements: List<Node>)
}

enum class HtmxSwap : HtmxSwapAction {
    InnerHtml {
        override fun performSwap(element: Element, newElements: List<Node>) {
            element.empty()
            element.appendChildren(newElements)
        }
    },
    OuterHtml {
        override fun performSwap(element: Element, newElements: List<Node>) {
            newElements.forEach { element.before(it) }
            element.remove()
        }
    },
    BeforeBegin {
        override fun performSwap(element: Element, newElements: List<Node>) {
            newElements.forEach { element.before(it) }
        }
    },
    AfterBegin {
        override fun performSwap(element: Element, newElements: List<Node>) {
            element.insertChildren(0, newElements)
        }
    },
    BeforeEnd {
        override fun performSwap(element: Element, newElements: List<Node>) {
            element.appendChildren(newElements)
        }
    },
    AfterEnd {
        override fun performSwap(element: Element, newElements: List<Node>) {
            newElements.reversed().forEach { element.after(it) }
        }
    },
    Delete {
        override fun performSwap(element: Element, newElements: List<Node>) {
            element.remove()
        }
    },
    None {
        override fun performSwap(element: Element, newElements: List<Node>) {

        }
    },
}


