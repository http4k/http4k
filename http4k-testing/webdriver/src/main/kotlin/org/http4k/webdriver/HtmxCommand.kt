package org.http4k.webdriver

import org.http4k.core.Method
import org.http4k.core.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.net.URLEncoder

data class HtmxCommand(
    val method: Method,
    val uri: String,
    val target: Element,
    val swap: HtmxSwap,
) {
    fun performOn(element: HtmxJsoupWebElement) {
        val response = element.handler(request(element.delegate.element))

        val mimeType = response.header("content-type")?.split(";")?.firstOrNull()

        val responseBody =
            when (mimeType) {
                "text/html" -> Jsoup.parse(response.bodyString(), Parser.xmlParser()).root().children()
                "text/plain" -> listOf(TextNode(response.bodyString()))
                null -> throw RuntimeException("No content type on response")
                else -> throw RuntimeException("Unsupported content type on response ${response.header("content-type")}")
            }

        swap
            .performSwap(
                element = target,
                newElements = responseBody ?: emptyList()
            )
    }

    private fun request(element: Element): Request {
        val formBody = formBodyOfElement(element)
        val isInput = listOf("input", "textarea", "select", "button").contains(element.tagName())

        return when {
            isInput && element.hasAttr("name") && method == Method.GET ->
                Request(Method.GET, uri).query(element.attr("name"), element.attr("value"))

            formBody != null && method == Method.GET ->
                Request(Method.GET, "$uri?$formBody")

            formBody != null ->
                Request(method, uri).body(formBody)

            else ->
                Request(method, uri)
        }
    }

    private fun formBodyOfElement(element: Element): String? =
        if (element.tagName() == "form")
            formBody(element)
        else
            element
                .parents()
                .toList()
                .firstOrNull { it.tagName() == "form" }
                ?.let { formBody(it) }

    private fun formBody(formElement: Element): String {
        // TODO: lots of duplication with JSoupWebElement
        // and slightly different approaches
        val inputs =
            formElement
                .getElementsByTag("input")
                .toList()
                .filter { it.hasAttr("name") }
                .map { it.attr("name") to it.attr("value") }

        val textAreas =
            formElement
                .getElementsByTag("textarea")
                .toList()
                .filter { it.hasAttr("name") }
                .map { it.attr("name") to it.text() }

        val selects =
            formElement
                .getElementsByTag("select")
                .toList()
                .filter { it.hasAttr("name") }
                .mapNotNull {
                    it.getElementsByTag("option")
                        .toList()
                        .find { option -> option.hasAttr("selected") }
                        ?.let { option -> it.attr("name") to option.attr("value") }
                }

        val buttons =
            formElement
                .getElementsByTag("button")
                .toList()
                .filter { it.hasAttr("name") }
                .map { it.attr("name") to it.attr("value") }

        val all = (inputs + textAreas + selects + buttons)

        return all.joinToString("&") {
            "${URLEncoder.encode(it.first, "UTF-8")}=${URLEncoder.encode(it.second, "UTF-8")}"
        }
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
                        else -> element.root().select(it).firstOrNull()
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

        fun from(element: HtmxJsoupWebElement): HtmxCommand? =
            fromElement(element.delegate.element)
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
        override fun performSwap(element: Element, newElements: List<Node>) {}
    },
}


