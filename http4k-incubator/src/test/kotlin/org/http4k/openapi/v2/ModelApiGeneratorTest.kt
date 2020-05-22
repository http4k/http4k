package org.http4k.openapi.v2

import org.http4k.openapi.ModelApiGeneratorContract

class ModelApiGeneratorTest : ModelApiGeneratorContract<OpenApi2Spec>(OpenApi2Spec::class, ModelApiGenerator)
