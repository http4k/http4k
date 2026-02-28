package org.http4k.wiretap.client

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode.inner
import org.http4k.datastar.Selector
import org.http4k.datastar.Signal
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.lens.datastarSignals
import org.http4k.lens.int
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.datastarModel

data class HeaderRowView(val key: String, val index: Int, val basePath: String)
data class HeaderRowsView(
    val rows: List<HeaderRowView>,
    val showAdd: Boolean,
    val basePath: String
) : ViewModel

fun headerRowsView(headers: Map<String, HeaderEntry>, basePath: String) =
    HeaderRowsView(
        rows = headers.entries.toList().mapIndexed { index, (key, _) -> HeaderRowView(key, index, basePath) },
        showAdd = headers.size < 10,
        basePath = basePath
    )

fun HeaderRows(elements: DatastarElementRenderer, basePath: String = "/_wiretap/inbound/"): RoutingHttpHandler =
    "headers" bind routes(
        "/reset" bind GET to {
            val newHeaders = mapOf("0" to HeaderEntry())
            Response(OK)
                .datastarSignals(Signal.of("""{"headers":null}"""))
                .datastarSignals(Signal.of(Json.asFormatString(mapOf("headers" to newHeaders))))
                .datastarElements(
                    elements(headerRowsView(newHeaders, basePath)),
                    morphMode = inner,
                    selector = Selector.of("#client-headers")
                )
        },
        "/add" bind GET to { req ->
            val model = req.datastarModel<ClientRequest>()
            val nextIndex = (model.headers.keys.mapNotNull { it.toIntOrNull() }.maxOrNull() ?: -1) + 1
            val newHeaders = model.headers + (nextIndex.toString() to HeaderEntry())
            Response(OK)
                .datastarSignals(Signal.of(Json.asFormatString(mapOf("headers" to newHeaders))))
                .datastarElements(
                    elements(headerRowsView(newHeaders, basePath)),
                    morphMode = inner,
                    selector = Selector.of("#client-headers")
                )
        },
        "/remove/{n}" bind GET to { req ->
            val n = Path.int().of("n")(req)
            val model = req.datastarModel<ClientRequest>()
            if (model.headers.size <= 1) return@to Response(OK)

            val remaining = model.headers.entries.toList()
                .filterIndexed { index, _ -> index != n }
                .associate { (key, entry) -> key to entry }

            Response(OK)
                .datastarSignals(Signal.of("""{"headers":null}"""))
                .datastarSignals(Signal.of(Json.asFormatString(mapOf("headers" to remaining))))
                .datastarElements(
                    elements(headerRowsView(remaining, basePath)),
                    morphMode = inner,
                    selector = Selector.of("#client-headers")
                )
        }
    )
