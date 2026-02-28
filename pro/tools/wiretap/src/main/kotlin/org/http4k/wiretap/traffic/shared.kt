package org.http4k.wiretap.traffic

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.domain.View
import org.http4k.wiretap.domain.ViewId

data class ViewSignals(
    val name: String? = null,
    val direction: String? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null
) {
    val normalizedFilter get() = TransactionFilterSignals(direction, host, method, status, path).toFilter()
}

fun DatastarElementRenderer.renderViewBar(views: List<View>) =
    Response(OK).datastarElements(
        this(ViewBarView(views.map { it.toButtonView() })),
        MorphMode.inner,
        Selector.of("#tab-bar")
    )

data class ViewButtonView(
    val id: ViewId,
    val name: String,
    val builtIn: Boolean,
    val filter: TransactionFilterSignals
)

data class ViewBarView(val tabs: List<ViewButtonView>) : ViewModel

fun View.toButtonView() = ViewButtonView(
    id = id,
    name = name,
    builtIn = builtIn,
    filter = TransactionFilterSignals(filter)
)

fun statusClass(code: Int) = when {
    code >= 500 -> "status-5xx"
    code >= 400 -> "status-4xx"
    code >= 300 -> "status-3xx"
    else -> "status-2xx"
}
