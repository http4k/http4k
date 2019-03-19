package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Gson
import org.http4k.format.Xml.asXmlToJsonElement
import java.io.PrintStream
import java.util.Random

class GenerateXmlDataClasses(out: PrintStream = System.out,
                             idGenerator: () -> Int = { Math.abs(Random().nextInt()) }) : Filter {
    private val chains = GenerateDataClasses(Gson, out, idGenerator).then(Filter { next ->
        {
            next(it).run { with(Gson.body().toLens() of bodyString().asXmlToJsonElement()) }
        }
    })

    override fun invoke(p1: HttpHandler): HttpHandler = { chains.then(p1)(it) }
}
