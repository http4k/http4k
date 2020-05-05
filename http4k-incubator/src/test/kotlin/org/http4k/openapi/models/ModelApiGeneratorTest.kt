package org.http4k.openapi.models

import org.http4k.junit.ResourceLoader
import org.http4k.junit.TestResources
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class, TestResources::class)
class ModelApiGeneratorTest {

    @Test
    fun `generates model class for simple object`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ModelApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates model class for nested object`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ModelApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates model class for ref`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ModelApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates model class for array of primitives`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ModelApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ModelApiGenerator, resourceLoader.text("openApi.json"))
    }

    @Test
    fun `generates model class for nested array of primitives`(approver: Approver, resourceLoader: ResourceLoader) {
        approver.assertGeneratedContent(ModelApiGenerator, resourceLoader.text("openApi.json"))
    }
}
