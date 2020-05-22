package org.http4k.openapi.v2

import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApiJson.asA
import java.io.File

fun main() {
    val generationOptions = GenerationOptions("org.http4k", File("http4k/src/main/kotlin"))
    val targetGeneratedDir = generationOptions.destinationFolder.apply { mkdirs() }
    val spec = File("apiSpec.json").readText().asA(OpenApi2Spec::class)

    println("Generating files to: " + targetGeneratedDir.absolutePath)

    listOf(ModelApiGenerator, ClientApiGenerator, ServerApiGenerator)
        .flatMap { it(spec, generationOptions) }
        .forEach { it.writeTo(targetGeneratedDir) }
}

