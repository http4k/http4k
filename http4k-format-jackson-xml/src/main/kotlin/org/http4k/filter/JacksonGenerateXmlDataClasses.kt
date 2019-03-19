package org.http4k.filter

import org.http4k.format.Jackson
import org.http4k.format.JacksonXml
import java.io.PrintStream
import java.util.Random

/**
 * Provides an implementation of GenerateXmlDataClasses using GSON as an engine.
 */

object JacksonGenerateXmlDataClasses {
    operator fun invoke(out: PrintStream = System.out,
                        idGenerator: () -> Int = { Math.abs(Random().nextInt()) }) =
        GenerateXmlDataClasses(Jackson, out, {
            Jackson.asJsonObject(JacksonXml.asA(it, Map::class))
        }, idGenerator)
}