package org.http4k.wiretap.home

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

fun Index(templates: TemplateRenderer): RoutingHttpHandler =
    "/" bind GET to { Response(OK).html(templates(Index)) }

data object Index : ViewModel
