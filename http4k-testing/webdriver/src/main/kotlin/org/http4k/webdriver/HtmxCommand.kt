package org.http4k.webdriver

import org.http4k.core.Method
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

data class HtmxCommand(
    val method: Method,
    val uri: String,
    val target: String?,
    val swap: HtmxSwap,
) {

    fun performOn(element: HtmxJsoupWebElement) {
        val response = element.handler(org.http4k.core.Request(method, uri))
        val responseBody = Jsoup.parse(response.bodyString()).getElementsByTag("body").first()

        swap
            .performSwap(
                element = element.delegate.element,
                responseNodes = responseBody?.childNodes() ?: emptyList()
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

        private fun fromElement(element: Element): HtmxCommand? =
            hxAttrs
                .firstOrNull { element.hasAttr(it.first) }
                ?.let {
                    HtmxCommand(
                        method = it.second,
                        uri = element.attr(it.first),
                        target = targetFromString(element.attr("hx-target"))
                            ?: targetFromString(element.attr("data-hx-target")),
                        swap = swapFromString(element.attr("hx-swap")) ?: swapFromString(element.attr("data-hx-swap"))
                        ?: HtmxSwap.InnerHtml
                    )
                }

        private fun targetFromString(s: String): String? =
            s.trim().ifEmpty { null }

        private fun swapFromString(s: String): HtmxSwap? =
            HtmxSwap
                .entries
                .firstOrNull() { s.lowercase().startsWith(it.toString().lowercase()) }
    }
}

interface HtmxSwapAction {
    fun performSwap(element: Element, responseNodes: List<Node>)
}

enum class HtmxSwap : HtmxSwapAction {
    InnerHtml {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            element.empty()
            element.appendChildren(responseNodes)
        }
    },
    OuterHtml {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            responseNodes.forEach { element.before(it) }
            element.remove()
        }
    },
    BeforeBegin {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            responseNodes.forEach { element.before(it) }
        }
    },
    AfterBegin {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            element.insertChildren(0, responseNodes)
        }
    },
    BeforeEnd {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            element.appendChildren(responseNodes)
        }
    },
    AfterEnd {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            responseNodes.reversed().forEach { element.after(it) }
        }
    },
    Delete {
        override fun performSwap(element: Element, responseNodes: List<Node>) {
            element.remove()
        }
    },
    None {
        override fun performSwap(element: Element, responseNodes: List<Node>) {

        }
    },
}


