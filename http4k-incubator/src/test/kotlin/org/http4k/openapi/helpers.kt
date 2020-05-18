package org.http4k.openapi

import org.http4k.openapi.OpenApiJson.asA
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import java.io.File

inline fun <reified T : Any> Approver.assertGeneratedContent(generator: ApiGenerator<T>, content: String) {
    assertApproved(generator(content.asA(T::class), GenerationOptions("testPackage", File(".")))
        .sortedBy { it.name }
        .joinToString("\n") { ">>>${it.name}.kt\n\n$it" })
}
