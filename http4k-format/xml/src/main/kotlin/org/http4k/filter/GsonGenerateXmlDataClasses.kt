package org.http4k.filter

import org.http4k.format.Gson
import org.http4k.format.Xml
import java.io.PrintStream
import java.util.Random
import kotlin.math.abs

/**
 * Provides an implementation of GenerateXmlDataClasses using GSON as an engine.
 */
object GsonGenerateXmlDataClasses {
    operator fun invoke(out: PrintStream = System.out,
                        idGenerator: () -> Int = { abs(Random().nextInt()) }) =
        GenerateXmlDataClasses(Gson, Xml, out, idGenerator)
}
