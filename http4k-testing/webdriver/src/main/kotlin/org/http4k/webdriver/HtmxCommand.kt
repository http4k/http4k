package org.http4k.webdriver

import org.http4k.core.Method
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class HtmxCommand(
    val method: Method,
    val uri: String,
    val target: String?,
    val swap: HtmxSwap,
) {

    fun performOn(element: HtmxJsoupWebElement) {
        val response = element.handler(org.http4k.core.Request(method, uri))
        val responseBody = Jsoup.parse(response.bodyString()).getElementsByTag("body").first()

        element.delegate.element.empty()

        if (responseBody != null) {
            element.delegate.element.appendChildren(responseBody.childNodes())
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

        fun from(element: HtmxJsoupWebElement): HtmxCommand? =
            fromElement(element.delegate.element)

        private fun fromElement(element: Element): HtmxCommand? =
            hxAttrs
                .firstOrNull { element.hasAttr(it.first) }
                ?.let {
                    HtmxCommand(
                        method = it.second,
                        uri = element.attr(it.first),
                        target = targetFromString(element.attr("hx-target")) ?: targetFromString(element.attr("data-hx-target")),
                        swap = swapFromString(element.attr("hx-swap")) ?: swapFromString(element.attr("data-hx-swap")) ?: HtmxSwap.InnerHtml
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

enum class HtmxSwap {
    InnerHtml,
    OuterHtml,
    BeforeBegin,
    AfterBegin,
    BeforeEnd,
    AfterEnd,
    Delete,
    None,
}


