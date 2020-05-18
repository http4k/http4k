package org.http4k.openapi.v2

import org.http4k.junit.ResourceLoader
import org.http4k.openapi.ApiGeneratorContract
import org.http4k.testing.Approver

class ServerApiGeneratorTest : ApiGeneratorContract<OpenApi2Spec>(OpenApi2Spec::class, ServerApiGenerator) {
    override fun `route with path`(app: Approver, rl: ResourceLoader) {
    }

}
