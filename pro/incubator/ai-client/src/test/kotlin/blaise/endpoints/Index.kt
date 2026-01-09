package blaise.endpoints

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

fun Index(renderer: TemplateRenderer) =
    "/" bind GET to { Response(OK).html(renderer(Index)) }


data object Index : ViewModel
