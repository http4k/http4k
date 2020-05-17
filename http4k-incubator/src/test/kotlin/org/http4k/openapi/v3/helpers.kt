package org.http4k.openapi.v3

import org.http4k.openapi.v3.OpenApiJson.asA
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import java.io.File

fun Approver.assertGeneratedContent(generator: ApiGenerator, content: String) {
    assertApproved(generator(content.asA(OpenApi3Spec::class), GenerationOptions("testPackage", File(".")))
        .sortedBy { it.name }
        .joinToString("\n") { ">>>${it.name}.kt\n\n$it" })
}
