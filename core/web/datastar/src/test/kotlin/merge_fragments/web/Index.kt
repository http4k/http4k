package merge_fragments.web

import merge_fragments.pages.Index
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.BiDiBodyLens
import org.http4k.routing.bind
import org.http4k.template.ViewModel

fun index(view: BiDiBodyLens<ViewModel>) = "/" bind {
    Response(OK).with(view of Index)
}
