package org.http4k.openapi.v3

import org.http4k.openapi.ApiGeneratorContract
import org.http4k.openapi.v3.client.ClientApiGenerator

class ClientApiGeneratorTest : ApiGeneratorContract<OpenApi3Spec>(OpenApi3Spec::class, ClientApiGenerator)
