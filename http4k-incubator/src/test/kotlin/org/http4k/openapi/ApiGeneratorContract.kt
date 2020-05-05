package org.http4k.openapi

import org.http4k.junit.ResourceLoader
import org.http4k.junit.TestResources
import org.http4k.openapi.client.ClientApiGenerator
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(ApprovalTest::class)
abstract class ApiGeneratorContract {

    @JvmField
    @RegisterExtension
    val resources = TestResources { "" }

    @Test
    fun `route with cookie`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `route with path`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `route with header`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `route with query`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }
}
