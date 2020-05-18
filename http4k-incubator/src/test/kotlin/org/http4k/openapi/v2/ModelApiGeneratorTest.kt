package org.http4k.openapi.v2

import org.http4k.openapi.ModelApiGeneratorContract
import org.junit.jupiter.api.Disabled

@Disabled
class ModelApiGeneratorTest : ModelApiGeneratorContract<OpenApi2Spec>(OpenApi2Spec::class, ModelApiGenerator)
