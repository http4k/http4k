package org.http4k.openapi.v2

import org.http4k.format.Jackson.asA
import org.http4k.openapi.GenerationOptions
import java.io.File

fun main() {
    val generationOptions = GenerationOptions("org.http4k", File("src/main/kotlin"))
    val targetGeneratedDir = generationOptions.destinationFolder.apply { mkdirs() }
    val spec = File("../apiSpec.json").readText().asA(OpenApi2Spec::class)

    println("Generating files to: " + targetGeneratedDir.absolutePath)

    listOf(ModelApiGenerator, ClientApiGenerator, ServerApiGenerator)
        .flatMap { it(spec, generationOptions) }
        .forEach { it.writeTo(targetGeneratedDir) }
}

