package org.http4k.openapi.v2

import org.http4k.openapi.ModelApiGeneratorContract
import org.http4k.openapi.v2.models.ModelApiGenerator

class ModelApiGeneratorTest : ModelApiGeneratorContract<OpenApi2Spec>(OpenApi2Spec::class, ModelApiGenerator)
