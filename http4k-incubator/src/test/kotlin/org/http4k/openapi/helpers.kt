package org.http4k.openapi

import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.OpenApiJson.asA
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import java.io.File

inline fun <reified T : Any> Approver.assertGeneratedContent(generator: ApiGenerator<T>, content: String) {
    assertApproved(generator(content.asA(T::class), GenerationOptions("testPackage", File(".")))
        .sortedBy { it.name }
        .joinToString("\n") { ">>>${it.name}.kt\n\n$it" })
}
