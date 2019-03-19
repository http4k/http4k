package org.http4k.filter

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.Jackson.json
import org.http4k.format.JacksonXml
import org.http4k.lens.BiDiBodyLens
import java.io.PrintStream
import java.util.Random

class GenerateXmlDataClasses(out: PrintStream = System.out,
                             idGenerator: () -> Int = { Math.abs(Random().nextInt()) }) : Filter {
    private val lens: BiDiBodyLens<JsonNode> = Body.json().toLens()
    private val chains = GenerateDataClasses(Jackson, out, idGenerator).then(Filter { next ->
        {
            next(it).run {
                with(lens of Jackson.asJsonObject(JacksonXml.asA(bodyString(), Map::class)))
            }
        }
    })

    override fun invoke(p1: HttpHandler): HttpHandler = { chains.then(p1)(it) }
}
