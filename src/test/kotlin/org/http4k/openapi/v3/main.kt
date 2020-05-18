package org.http4k.openapi.v3

import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApiJson.asA
import org.http4k.openapi.v3.client.ClientApiGenerator
import org.http4k.openapi.v3.models.ModelApiGenerator
import org.http4k.openapi.v3.server.ServerApiGenerator
import java.io.File

fun main() {
    val generationOptions = GenerationOptions("org.http4k", File("http4k/src/main/kotlin"))

    val targetGeneratedDir = generationOptions.destinationFolder.apply { mkdirs() }
    val spec = File("http4k/src/test/resources/org/http4k/openapi/v3/apiSpec.json").readText().asA(OpenApi3Spec::class)

    println("Generating files to: " + targetGeneratedDir.absolutePath)

    listOf(ModelApiGenerator, ClientApiGenerator, ServerApiGenerator)
        .flatMap { it(spec, generationOptions) }
        .forEach {
            it.writeTo(targetGeneratedDir)
        }
}

