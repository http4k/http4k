package org.http4k.filter

import org.http4k.format.Jackson
import org.http4k.format.JacksonXml
import java.io.PrintStream
import java.util.Random

/**
 * Provides an implementation of GenerateXmlDataClasses using GSON as an engine.
 */
object JacksonXmlGenerateXmlDataClasses {
    operator fun invoke(
        out: PrintStream = System.out,
        idGenerator: () -> Int = { Math.abs(Random().nextInt()) }
    ) =
        GenerateXmlDataClasses(Jackson, JacksonXml, out, idGenerator)
}
