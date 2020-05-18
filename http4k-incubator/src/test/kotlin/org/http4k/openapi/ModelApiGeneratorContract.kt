package org.http4k.openapi

import org.http4k.junit.ResourceLoader
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

abstract class ModelApiGeneratorContract<T : Any>(clazz: KClass<T>, apiGenerator: ApiGenerator<T>) : ApiGeneratorContract<T>(clazz, apiGenerator) {
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
