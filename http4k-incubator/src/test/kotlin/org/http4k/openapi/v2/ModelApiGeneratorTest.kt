package org.http4k.openapi.v2

import org.http4k.junit.ResourceLoader
import org.http4k.openapi.ModelApiGeneratorContract
import org.http4k.openapi.v2.models.ModelApiGenerator
import org.http4k.testing.Approver

class ModelApiGeneratorTest : ModelApiGeneratorContract<OpenApi2Spec>(OpenApi2Spec::class, ModelApiGenerator) {
    override fun `route with cookie`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with path`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with header`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with query`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with form body object`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with form body ref`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json response array`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json response object`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json response ref`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json body array`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json body object`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json body object additional properties`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json body object no schema`(app: Approver, rl: ResourceLoader) {
    }

    override fun `route with json body ref`(app: Approver, rl: ResourceLoader) {
    }
}
