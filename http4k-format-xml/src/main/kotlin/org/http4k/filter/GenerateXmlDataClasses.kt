package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Xml
import java.io.PrintStream
import java.util.*

class GenerateXmlDataClasses(out: PrintStream = System.out,
                             idGenerator: () -> Int = { Math.abs(Random().nextInt()) }) : Filter {

    private val chains = GenerateDataClasses(Jackson, out, idGenerator).then(Filter { next ->
        {
            val originalResponse = next(it)
            originalResponse.with(Jackson.body().toLens() of (Xml.mapper.readTree(originalResponse.bodyString())))
        }
    })

    override fun invoke(p1: HttpHandler): HttpHandler = { chains.then(p1)(it) }
}
