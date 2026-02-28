package org.http4k.wiretap.client

import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Signal
import org.http4k.lens.datastarSignals
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.wiretap.util.Json
import org.http4k.wiretap.util.Json.datastarModel
import org.http4k.wiretap.util.formatBody

fun FormatBody(): RoutingHttpHandler = "format" bind POST to { req ->
    val model = req.datastarModel<ClientRequest>()
    val formatted = formatBody(model.body, model.contentType)
    Response(OK).datastarSignals(Signal.of(Json.asFormatString(mapOf("body" to formatted))))
}
