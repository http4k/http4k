package org.http4k.wiretap.client

import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.wiretap.util.SignalModel
import org.http4k.wiretap.util.datastarSignal
import org.http4k.wiretap.util.formatBody

data class FormattedBodySignals(val body: String) : SignalModel

fun FormatBody(): RoutingHttpHandler = "format" bind POST to { req ->
    val model = clientRequestLens(req)
    val formatted = formatBody(model.body, model.contentType)
    Response(OK).datastarSignal(FormattedBodySignals(formatted))
}
