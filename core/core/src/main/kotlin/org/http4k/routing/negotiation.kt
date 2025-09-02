package org.http4k.routing

import org.http4k.core.ContentEncodingName
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage
import org.http4k.core.PriorityList
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.preferred
import org.http4k.core.with
import org.http4k.lens.ACCEPT_ENCODING
import org.http4k.lens.ACCEPT_LANGUAGE
import org.http4k.lens.BiDiLens
import org.http4k.lens.CONTENT_ENCODING
import org.http4k.lens.CONTENT_LANGUAGE
import org.http4k.lens.Header
import org.http4k.lens.Lens
import java.util.Locale

/**
 * A generic implementation of routing by proactive content negotiation.
 * See RFC 9110, Section 12.1
 */
fun <Range : Any, OptionId : Any> proactiveContentNegotiation(
    acceptBy: Lens<HttpMessage, PriorityList<Range>?>,
    reportBy: BiDiLens<HttpMessage, OptionId?>,
    match: (Range, OptionId) -> Boolean,
    bindings: List<Pair<OptionId, HttpHandler>>
): HttpHandler {
    return { request ->
        val selected = when (val priorityList = acceptBy(request)) {
            null -> bindings.first()
            else -> priorityList.preferred(bindings, match, { it.first })
        }
        
        val response = when (selected) {
            null -> Response(NOT_ACCEPTABLE)
            else -> {
                val (id, handler) = selected
                handler(request).with(reportBy of id)
            }
        }
        
        response.header("vary", acceptBy.meta.name)
    }
}

/**
 * Route by proactive negotiation of content language.
 */
fun contentLanguages(routes: List<Pair<Locale, HttpHandler>>) =
    proactiveContentNegotiation(
        acceptBy = Header.ACCEPT_LANGUAGE,
        reportBy = Header.CONTENT_LANGUAGE,
        match = { r, o -> r.matches(o) },
        routes
    )

/**
 * Route by proactive negotiation of content language.
 */
fun contentLanguages(vararg routes: Pair<Locale, HttpHandler>) =
    contentLanguages(routes.toList())
