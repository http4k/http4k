package chatzilla.endpoints

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel

fun GetMessageForm(renderer: DatastarElementRenderer) =
    "/message" bind GET to { Response(OK).datastarElements(renderer(MessageForm)) }

data object MessageForm : ViewModel

data object DisabledMessageForm : ViewModel
