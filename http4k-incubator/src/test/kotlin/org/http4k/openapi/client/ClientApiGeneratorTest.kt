package org.http4k.openapi.client

import org.http4k.junit.ResourceLoader
import org.http4k.junit.TestResources
import org.http4k.openapi.models.assertGeneratedContent
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class, TestResources::class)
class ClientApiGeneratorTest {

    @Test
    fun `generates route with cookie`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates route with path`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates route with header`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates route with query`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ClientApiGenerator, resourceLoader.text("openApi.json"))
    }
}
