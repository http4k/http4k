package org.http4k.openapi.v3

import org.http4k.junit.ResourceLoader
import org.http4k.openapi.v3.models.ModelApiGenerator
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test

class ModelApiGeneratorTest : ApiGeneratorContract(ModelApiGenerator) {

    @Test
    fun `generates model class for simple object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `generates model class for nested object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `generates model class for ref`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `generates model class for array of primitives`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `generates model class for mixed array`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `generates model class for nested array of primitives`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)
}
