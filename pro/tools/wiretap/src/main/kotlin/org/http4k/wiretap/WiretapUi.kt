package org.http4k.wiretap

import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.home.Index

fun WiretapUi(
    renderer: DatastarElementRenderer,
    html: TemplateRenderer,
    functions: List<WiretapFunction>,
) = routes(
    "/__wiretap" bind routes(
        listOf(
            Index(html),
            static(Classpath("public"))
        ) + functions.map { it.http(renderer, html) },
    )
)
