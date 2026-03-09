/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.traffic

import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.View
import org.http4k.wiretap.domain.ViewId
import org.http4k.wiretap.util.SignalModel
import org.http4k.wiretap.util.auto

val filterLens = Body.auto<TransactionFilterSignals>()
val viewSignalsLens = Body.auto<ViewSignals>()

data class ViewSignals(
    val name: String? = null,
    val direction: String? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null
) {
    val normalized get() = TransactionFilterSignals(direction, host, method, status, path).toFilter()
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
    val builtIn: Boolean
)

data class ViewBarView(val tabs: List<ViewButtonView>) : ViewModel

fun View.toButtonView() = ViewButtonView(
    id = id,
    name = name,
    builtIn = builtIn
)

data class ViewActivationSignals(
    val activeView: String,
    val customView: Boolean,
    val direction: String = "",
    val host: String = "",
    val method: String = "",
    val status: String = "",
    val path: String = ""
) : SignalModel {
    constructor(view: View) : this(
        activeView = view.id.toString(),
        customView = !view.builtIn,
        direction = view.filter.direction?.name ?: "",
        host = view.filter.host ?: "",
        method = view.filter.method?.name ?: "",
        status = view.filter.status?.code?.toString() ?: "",
        path = view.filter.path ?: ""
    )

    companion object {
        val Reset = ViewActivationSignals(activeView = "all", customView = false)
    }
}

data class SaveViewSignals(
    val showAddView: Boolean = false,
    val name: String = ""
) : SignalModel

fun statusClass(code: Int) = when {
    code >= 500 -> "status-5xx"
    code >= 400 -> "status-4xx"
    code >= 300 -> "status-3xx"
    else -> "status-2xx"
}

