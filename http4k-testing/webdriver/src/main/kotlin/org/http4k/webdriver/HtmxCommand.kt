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
        val response = element.handler(org.http4k.core.Request(Method.GET, uri))
        val responseBody = Jsoup.parse(response.bodyString()).getElementsByTag("body").first()

        element.delegate.element.empty()

        if (responseBody != null) {
            element.delegate.element.appendChildren(responseBody.childNodes())
        }
    }

    companion object {
        private val hxAttrs = mapOf(
            "hx-get" to Method.GET,
            "data-hx-get" to Method.GET,
        )

        fun from(element: HtmxJsoupWebElement): HtmxCommand? =
            fromElement(element.delegate.element)

        private fun fromElement(element: Element): HtmxCommand? =
            hxAttrs
                .entries
                .firstOrNull { element.hasAttr(it.key) }
                ?.let {
                    HtmxCommand(
                        method = it.value,
                        uri = element.attr(it.key),
                        target = targetFromString(element.attr("hx-target")),
                        swap = swapFromString(element.attr("hx-swap"))
                    )
                }

        private fun targetFromString(s: String): String? =
            s.trim().ifEmpty { null }

        private fun swapFromString(s: String): HtmxSwap =
            HtmxSwap
                .entries
                .firstOrNull() { it.toString().lowercase() == s.lowercase() }
                ?: HtmxSwap.InnerHtml
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


