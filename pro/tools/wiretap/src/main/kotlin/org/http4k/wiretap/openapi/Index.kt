package org.http4k.wiretap.openapi

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.wiretap.chaos.ChaosConfigSignals
import org.http4k.wiretap.domain.ChaosConfig
import org.http4k.wiretap.util.Json

fun Index(templates: TemplateRenderer): RoutingHttpHandler =
    "/" bind GET to { Response(OK).html(templates(Index)) }

data object Index : ViewModel
