package org.http4k.openapi.v3

import org.http4k.junit.ResourceLoader
import org.http4k.openapi.ApiGeneratorContract
import org.http4k.openapi.assertGeneratedContent
import org.http4k.openapi.v3.models.ModelApiGenerator
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class ModelApiGeneratorTest : ApiGeneratorContract<OpenApi3Spec>(OpenApi3Spec::class, ModelApiGenerator) {

    @Test
    fun `model simple object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `model nested object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `model ref`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `model array of primitives`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `model mixed array`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `model nested array of primitives`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)
}
