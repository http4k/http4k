package org.http4k.openapi.models

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.OpenApiJson.asA
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import java.io.File

fun Approver.assertGeneratedContent(generator: ApiGenerator, content: String) {
    val modelApiGenerator = generator(content.asA(OpenApi3Spec::class), GenerationOptions("testPackage", File(".")))
    assertThat(modelApiGenerator.map { it.name }, equalTo(listOf("Obj")))
    assertApproved(modelApiGenerator[0].toString())
}
