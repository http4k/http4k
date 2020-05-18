package org.http4k.openapi.v3

import org.http4k.openapi.ApiGeneratorContract
import org.http4k.openapi.v3.server.ServerApiGenerator

class ServerApiGeneratorTest : ApiGeneratorContract<OpenApi3Spec>(OpenApi3Spec::class, ServerApiGenerator)
