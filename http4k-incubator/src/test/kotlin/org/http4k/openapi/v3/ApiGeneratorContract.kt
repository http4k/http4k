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
    fun `route with cookie`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with path`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with header`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with query`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with form body object`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with form body ref`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

//    @Test
//    fun `route with json response array`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with json response object`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with json response ref`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body array`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body object`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body object additional properties`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    @Test
    fun `route with json body ref`(app: Approver, rl: ResourceLoader)  = app.assertGeneratedContent(rl)

    fun Approver.assertGeneratedContent(rl: ResourceLoader) {
        assertGeneratedContent(apiGenerator, rl.text("openApi.json"))
    }
}
