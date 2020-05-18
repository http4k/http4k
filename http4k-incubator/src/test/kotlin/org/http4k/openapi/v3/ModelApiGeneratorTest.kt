package org.http4k.openapi.v3

import org.http4k.openapi.ModelApiGeneratorContract
import org.http4k.openapi.v3.models.ModelApiGenerator

class ModelApiGeneratorTest : ModelApiGeneratorContract<OpenApi3Spec>(OpenApi3Spec::class, ModelApiGenerator)
