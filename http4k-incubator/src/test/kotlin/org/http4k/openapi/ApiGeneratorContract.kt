package org.http4k.openapi

import org.http4k.junit.ResourceLoader
import org.http4k.junit.TestResources
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import kotlin.reflect.KClass

@ExtendWith(ApprovalTest::class)
abstract class ApiGeneratorContract<T : Any>(private val clazz: KClass<T>, private val apiGenerator: ApiGenerator<T>) {

    @JvmField
    @RegisterExtension
    val resources = TestResources { "" }

    @Test
    fun `route with cookie`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with path`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with header`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with query`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with form body object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with form body ref`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json response array`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json response object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json response ref`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body array`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body object`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body object additional properties`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body object no schema`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body ref`(app: Approver, rl: ResourceLoader) = app.assertGeneratedContent(rl)

    fun Approver.assertGeneratedContent(rl: ResourceLoader) {
        assertApproved(apiGenerator(OpenApiJson.asA(rl.text("openApi.json"), clazz), GenerationOptions("testPackage", File(".")))
            .sortedBy { it.name }
            .joinToString("\n") { ">>>${it.name}.kt\n\n$it" })
    }
}
