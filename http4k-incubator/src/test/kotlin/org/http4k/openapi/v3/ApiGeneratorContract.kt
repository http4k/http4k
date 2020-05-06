package org.http4k.openapi.v3

import org.http4k.junit.ResourceLoader
import org.http4k.junit.TestResources
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(ApprovalTest::class)
abstract class ApiGeneratorContract(private val apiGenerator: ApiGenerator) {

    @JvmField
    @RegisterExtension
    val resources = TestResources { "" }

    @Test
    fun `route with cookie`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(apiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `route with path`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(apiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `route with header`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(apiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `route with query`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(apiGenerator, resourceLoader.text("openApi.json"))
    }
//
//    @Test
//    fun `route with form body`(approver: Approver, resourceLoader: ResourceLoader) {
//        approver.assertGeneratedContent(apiGenerator, resourceLoader.text("openApi.json"))
//    }
}
